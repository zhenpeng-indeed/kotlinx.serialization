/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.test.isJs
import kotlin.reflect.typeOf
import kotlin.test.*

@UseExperimental(ImplicitReflectionSerializer::class)
class TypeOfSerializerLookupTest {

    private inline fun <reified T> assertSerializedWithType(
        expected: String,
        obj: T,
        json: StringFormat = Json.unquoted
    ) {
        val serial = serializer<T>()
        assertEquals(expected, json.stringify(serial, obj))
    }

    @Test
    fun testPrimitive() {
        val token = typeOf<Int>()
        val serial = serializer(token)
        assertSame(IntSerializer as KSerializer<*>, serial)
        assertSerializedWithType("42", 42)
    }

    @Test
    fun testPlainObject() {
        val b = StringData("some string")
        assertSerializedWithType("""{data:"some string"}""", b)
    }

    @Test
    fun testListWithT() {
        val source = """[{"intV":42}]"""
        val serial = serializer<List<IntData>>()
        assertEquals(listOf(IntData(42)), Json.parse(serial, source))
    }

    @Test
    fun testPrimitiveList() {
        val myArr = listOf("a", "b", "c")
        assertSerializedWithType("[a,b,c]", myArr)
    }

    @Test
    fun testPrimitiveSet() {
        val mySet = setOf("a", "b", "c", "c")
        assertSerializedWithType("[a,b,c]", mySet)
    }

    @Test
    fun testMapWithT() {
        val myMap = mapOf("string" to StringData("foo"), "string2" to StringData("bar"))
        assertSerializedWithType("""{string:{data:foo},string2:{data:bar}}""", myMap)
    }

    @Test
    fun testNestedLists() {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testListSubtype() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<ArrayList<Int>>("[1,2,3]", myList)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
    }

    @Test
    fun testListProjection() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
        assertSerializedWithType<MutableList<out Int>>("[1,2,3]", myList)
        assertSerializedWithType<ArrayList<in Int>>("[1,2,3]", myList)
    }

    @Test
    fun testNullableTypes() {
        val myList: List<Int?> = listOf(1, null, 3)
        assertSerializedWithType("[1,null,3]", myList)
        assertSerializedWithType<List<Int?>?>("[1,null,3]", myList)
    }

    // Tests with [constructSerializerForGivenTypeArgs] are unsupported on Kotlin/JS
    private inline fun noJs(test: () -> Unit) {
        if (!isJs()) test()
    }

    @Test
    fun testCustomGeneric() = noJs {
        val intBox = Box(42)
        val intBoxSerializer = serializer<Box<Int>>()
        assertEquals(Box.serializer(IntSerializer).descriptor, intBoxSerializer.descriptor)
        assertSerializedWithType("""{boxed:42}""", intBox)
        val dataBox = Box(StringData("foo"))
        assertSerializedWithType("""{boxed:{data:foo}}""", dataBox)
    }

    @Test
    fun testRecursiveGeneric() = noJs {
        val boxBox = Box(Box(Box(IntData(42))))
        assertSerializedWithType("""{boxed:{boxed:{boxed:{intV:42}}}}""", boxBox)
    }

    @Test
    fun testMixedGeneric() = noJs {
        val listOfBoxes = listOf(Box("foo"), Box("bar"))
        assertSerializedWithType("""[{boxed:foo},{boxed:bar}]""", listOfBoxes)
        val boxedList = Box(listOf("foo", "bar"))
        assertSerializedWithType("""{boxed:[foo,bar]}""", boxedList)
    }
}