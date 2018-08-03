/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinType

class JsIntrinsics(private val irBuiltIns: IrBuiltIns, val context: JsIrBackendContext) {

    // Equality operations:

    val jsEqeq = binOpBool("jsEqeq")
    val jsNotEq = binOpBool("jsNotEq")
    val jsEqeqeq = binOpBool("jsEqeqeq")
    val jsNotEqeq = binOpBool("jsNotEqeq")

    val jsGt = binOpBool("jsGt")
    val jsGtEq = binOpBool("jsGtEq")
    val jsLt = binOpBool("jsLt")
    val jsLtEq = binOpBool("jsLtEq")


    // Unary operations:

    val jsNot = unOpBool("jsNot")

    val jsUnaryPlus = unOp("jsUnaryPlus")
    val jsUnaryMinus = unOp("jsUnaryMinus")

    val jsPrefixInc = unOp("jsPrefixInc")
    val jsPostfixInc = unOp("jsPostfixInc")
    val jsPrefixDec = unOp("jsPrefixDec")
    val jsPostfixDec = unOp("jsPostfixDec")

    // Binary operations:

    val jsPlus = binOp("jsPlus")
    val jsMinus = binOp("jsMinus")
    val jsMult = binOp("jsMult")
    val jsDiv = binOp("jsDiv")
    val jsMod = binOp("jsMod")

    val jsAnd = binOp("jsAnd")
    val jsOr = binOp("jsOr")


    // Bit operations:

    val jsBitAnd = binOpInt("jsBitAnd")
    val jsBitOr = binOpInt("jsBitOr")
    val jsBitXor = binOpInt("jsBitXor")
    val jsBitNot = unOpInt("jsBitNot")

    val jsBitShiftR = binOpInt("jsBitShiftR")
    val jsBitShiftRU = binOpInt("jsBitShiftRU")
    val jsBitShiftL = binOpInt("jsBitShiftL")


    // KFunction operations:

    val jsName = unOp("kCallableName", irBuiltIns.string)
    val jsPropertyGet = binOp("kPropertyGet")
    val jsPropertySet = tripleOp("kPropertySet", irBuiltIns.unit)


    // Type checks:

    val jsInstanceOf = binOpBool("jsInstanceOf")
    val jsTypeOf = unOp("jsTypeOf", irBuiltIns.string)


    // Number conversions:

    val jsNumberToByte = getInternalFunction("numberToByte")
    val jsNumberToDouble = getInternalFunction("numberToDouble")
    val jsNumberToInt = getInternalFunction("numberToInt")
    val jsNumberToShort = getInternalFunction("numberToShort")
    val jsNumberToLong = getInternalFunction("numberToLong")
    val jsToByte = getInternalFunction("toByte")
    val jsToShort = getInternalFunction("toShort")
    val jsToLong = getInternalFunction("toLong")


    // RTTI:

    val isInterfaceSymbol = getInternalFunction("isInterface")
    val isArraySymbol = getInternalFunction("isArray")
    //    val isCharSymbol = getInternalFunction("isChar")
    val isObjectSymbol = getInternalFunction("isObject")

    // Other:

    val jsObjectCreate = defineObjectCreateIntrinsic() // Object.create
    val jsSetJSField = defineSetJSPropertyIntrinsic() // till we don't have dynamic type we use intrinsic which sets a field with any name
    val jsToJsType = defineToJsType() // creates name reference to KotlinType
    val jsCode = getInternalFunction("js") // js("<code>")
    val jsHashCode = getInternalFunction("hashCode")
    val jsToString = getInternalFunction("toString")
    val jsCompareTo = getInternalFunction("compareTo")
    val jsEquals = getInternalFunction("equals")

    val jsNumberRangeToNumber = getInternalFunction("numberRangeToNumber")
    val jsNumberRangeToLong = getInternalFunction("numberRangeToLong")

    val longConstructor =
        context.symbolTable.referenceConstructor(context.getClass(FqName("kotlin.Long")).constructors.single())
    val longToDouble = context.symbolTable.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toDouble")
        )
    )
    val longToFloat = context.symbolTable.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toFloat")
        )
    )

    val charConstructor = context.symbolTable.referenceConstructor(context.getClass(KotlinBuiltIns.FQ_NAMES._char.toSafe()).constructors.single())

    // Helpers:

    private fun getInternalFunction(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    private fun defineToJsType() =
        JsIrBuilder.buildFunction("\$toJSType\$", origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            val typeParameter = JsIrBuilder.buildTypeParameter(Name.identifier("T"), 0, false)
            val anyType = irBuiltIns.anyType
            typeParameter.parent = it
            typeParameter.superTypes += anyType
            it.typeParameters += typeParameter
            it.returnType = anyType
        }

    // TODO: unify how we create intrinsic symbols
    private fun defineObjectCreateIntrinsic() =
        JsIrBuilder.buildFunction("Object\$create", isInline = true, origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            val typeParameter = JsIrBuilder.buildTypeParameter(Name.identifier("T"), 0, true)
            val anyType = irBuiltIns.anyType
            typeParameter.parent = it
            typeParameter.superTypes += anyType
            it.typeParameters += typeParameter
            it.returnType = anyType
        }

    private fun defineSetJSPropertyIntrinsic() =
        JsIrBuilder.buildFunction("\$setJSProperty\$", origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            it.returnType = irBuiltIns.unitType
            listOf("receiver", "fieldName", "fieldValue").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
        }

    private fun unOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN)) }

    private fun unOpBool(name: String) = unOp(name, irBuiltIns.bool)
    private fun unOpInt(name: String) = unOp(name, irBuiltIns.int)

    private fun binOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN)) }

    private fun tripleOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN, anyN)) }

    private fun binOpBool(name: String) = binOp(name, irBuiltIns.bool)
    private fun binOpInt(name: String) = binOp(name, irBuiltIns.int)
}
