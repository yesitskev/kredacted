package kredacted

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KredactedIrTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val redactedFqName = FqName("kredacted.Redacted")
    private val kredactedPackage = FqName("kredacted")
    private val redactedHelperClassId =
        ClassId(kredactedPackage, Name.identifier("RedactedHelper"))
    private val padDirectionClassId =
        ClassId(kredactedPackage, Name.identifier("PadDirection"))

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClass(declaration: IrClass): IrStatement {
        val classRedacted = declaration.getAnnotation(redactedFqName) != null

        val properties = declaration.declarations
            .filterIsInstance<IrProperty>()
            .filter { it.backingField != null && it.getter != null }

        val hasPropertyRedaction = properties.any { it.getAnnotation(redactedFqName) != null }

        if (!classRedacted && !hasPropertyRedaction) return super.visitClass(declaration)

        val toStringFun = declaration.declarations
            .filterIsInstance<IrSimpleFunction>()
            .find {
                it.name.asString() == "toString" &&
                    it.parameters.count { p -> p.kind.isValue() } == 0
            }
            ?: return super.visitClass(declaration)

        val dispatchReceiver = toStringFun.dispatchReceiverParameter
            ?: return super.visitClass(declaration)

        val redactFnSymbol = pluginContext.referenceFunctions(
            CallableId(redactedHelperClassId, Name.identifier("redact"))
        ).singleOrNull() ?: return super.visitClass(declaration)
        val helperClassSymbol = redactFnSymbol.owner.parentAsClass.symbol

        val anyToStringSymbol = pluginContext.irBuiltIns.anyClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .single { it.name.asString() == "toString" }
            .symbol

        val builder = DeclarationIrBuilder(pluginContext, toStringFun.symbol)
        toStringFun.body = builder.irBlockBody {
            +irReturn(
                irConcat().also { concat ->
                    concat.arguments.add(irString("${declaration.name.asString()}("))
                    properties.forEachIndexed { index, property ->
                        if (index > 0) concat.arguments.add(irString(", "))
                        concat.arguments.add(irString("${property.name.asString()}="))

                        if (classRedacted) {
                            concat.arguments.add(irString(CLASS_LEVEL_REPLACEMENT))
                            return@forEachIndexed
                        }

                        val getterCall = irCall(property.getter!!.symbol).also { call ->
                            call.arguments[0] = irGet(dispatchReceiver)
                        }
                        val toStringCall = irCall(anyToStringSymbol).also { call ->
                            call.arguments[0] = getterCall
                        }

                        val propertyAnnotation = property.getAnnotation(redactedFqName)
                        if (propertyAnnotation == null) {
                            concat.arguments.add(toStringCall)
                            return@forEachIndexed
                        }

                        val maskValue = propertyAnnotation.constStringArg(0) ?: DEFAULT_MASK
                        val padToLength = propertyAnnotation.constIntArg(1) ?: NO_PAD
                        val padDirectionExpr = propertyAnnotation.argAt(2)?.deepCopyWithSymbols()
                            ?: defaultPadDirectionExpr()

                        val redactCall = irCall(redactFnSymbol).also { call ->
                            call.arguments[0] = irGetObject(helperClassSymbol)
                            call.arguments[1] = toStringCall
                            call.arguments[2] = irString(maskValue)
                            call.arguments[3] = irInt(padToLength)
                            call.arguments[4] = padDirectionExpr
                        }
                        concat.arguments.add(redactCall)
                    }
                    concat.arguments.add(irString(")"))
                }
            )
        }
        return super.visitClass(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun defaultPadDirectionExpr(): IrExpression {
        val enumClassSymbol = pluginContext.referenceClass(padDirectionClassId)!!
        val startEntry = enumClassSymbol.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.asString() == "START" }
        return IrGetEnumValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            enumClassSymbol.defaultType,
            startEntry.symbol
        )
    }

    private fun IrConstructorCall.argAt(index: Int): IrExpression? =
        arguments.getOrNull(index)

    private fun IrConstructorCall.constStringArg(index: Int): String? =
        (argAt(index) as? IrConst)?.value as? String

    private fun IrConstructorCall.constIntArg(index: Int): Int? =
        (argAt(index) as? IrConst)?.value as? Int

    companion object {
        private const val CLASS_LEVEL_REPLACEMENT = "*****"
        private const val DEFAULT_MASK = "/./g"
        private const val NO_PAD = -1
    }
}

private fun org.jetbrains.kotlin.ir.declarations.IrParameterKind.isValue(): Boolean =
    this == org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular ||
        this == org.jetbrains.kotlin.ir.declarations.IrParameterKind.Context
