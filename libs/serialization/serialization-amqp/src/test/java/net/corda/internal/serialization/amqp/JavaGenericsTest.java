package net.corda.internal.serialization.amqp;

import net.corda.internal.serialization.amqp.testutils.TestSerializationContext;
import net.corda.sandbox.SandboxGroup;
import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.serialization.SerializedBytes;
import net.corda.internal.serialization.amqp.custom.BigIntegerSerializer;
import net.corda.internal.serialization.amqp.testutils.AMQPTestUtilsKt;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.NotSerializableException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.corda.internal.serialization.amqp.testutils.AMQPTestUtilsKt.testDefaultFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unchecked")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class JavaGenericsTest {
    @CordaSerializable
    private static class Inner {
        private final Integer v;

        public Inner(Integer v) {
            this.v = v;
        }

        Integer getV() {
            return v;
        }
    }

    @CordaSerializable
    private static class A<T> {
        private final T t;

        public A(T t) {
            this.t = t;
        }

        public T getT() {
            return t;
        }
    }

    @CordaSerializable
    private static class ConcreteClass {
        private final String theItem;

        public ConcreteClass(String theItem) {
            this.theItem = theItem;
        }

        public String getTheItem() {
            return theItem;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConcreteClass that = (ConcreteClass) o;
            return Objects.equals(theItem, that.theItem);
        }

        @Override
        public int hashCode() {

            return Objects.hash(theItem);
        }
    }


    @CordaSerializable
    private static class GenericClassWithList<CC> {
        private final List<CC> items;

        public GenericClassWithList(List<CC> items) {
            this.items = items;
        }

        public List<CC> getItems() {
            return items;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericClassWithList<?> that = (GenericClassWithList<?>) o;
            return Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {

            return Objects.hash(items);
        }
    }

    @CordaSerializable
    private static class GenericClassWithMap<CC, GG> {
        private final Map<CC, GG> theMap;

        public GenericClassWithMap(Map<CC, GG> theMap) {
            this.theMap = new LinkedHashMap<>(theMap);
        }

        public Map<CC, GG> getTheMap() {
            return theMap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericClassWithMap<?, ?> that = (GenericClassWithMap<?, ?>) o;
            return Objects.equals(theMap, that.theMap);
        }

        @Override
        public int hashCode() {

            return Objects.hash(theMap);
        }
    }

    @CordaSerializable
    private static class HolderOfGeneric<G> {
        private final G theGeneric;

        public HolderOfGeneric(G theGeneric) {
            this.theGeneric = theGeneric;
        }

        public G getTheGeneric() {
            return theGeneric;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HolderOfGeneric<?> that = (HolderOfGeneric<?>) o;
            return Objects.equals(theGeneric, that.theGeneric);
        }

        @Override
        public int hashCode() {
            return Objects.hash(theGeneric);
        }
    }

    @Test
    public void shouldSupportNestedGenericsFromJavaWithCollections() throws NotSerializableException {
        ConcreteClass concreteClass = new ConcreteClass("How to make concrete, $99/class");
        HolderOfGeneric<GenericClassWithList<ConcreteClass>> genericList = new HolderOfGeneric<>(new GenericClassWithList<>(Collections.singletonList(concreteClass)));
        SerializerFactory factory = AMQPTestUtilsKt.testDefaultFactory();
        SerializationOutput ser = new SerializationOutput(factory);
        SerializedBytes<?> bytes = ser.serialize(genericList, TestSerializationContext.testSerializationContext);
        DeserializationInput des = new DeserializationInput(factory);
        HolderOfGeneric<GenericClassWithList<ConcreteClass>> genericList2 = des.deserialize(bytes, HolderOfGeneric.class, TestSerializationContext.testSerializationContext);
        assertThat(genericList, CoreMatchers.is(CoreMatchers.equalTo(genericList2)));
    }

    @Test
    public void shouldSupportNestedGenericsFromJavaWithMaps() throws NotSerializableException {
        ConcreteClass concreteClass = new ConcreteClass("How to make concrete, $99/class");
        GenericClassWithMap<ConcreteClass, BigInteger> genericMap = new GenericClassWithMap<>(Collections.singletonMap(concreteClass, BigInteger.ONE));
        SerializerFactory factory = AMQPTestUtilsKt.testDefaultFactory();
        factory.register(new BigIntegerSerializer(), factory);
        SerializationOutput ser = new SerializationOutput(factory);
        SerializedBytes<?> bytes = ser.serialize(genericMap, TestSerializationContext.testSerializationContext);
        DeserializationInput des = new DeserializationInput(factory);
        GenericClassWithMap<ConcreteClass, BigInteger> genericMap2 = des.deserialize(bytes, GenericClassWithMap.class, TestSerializationContext.testSerializationContext);
        assertThat(genericMap2, CoreMatchers.is(CoreMatchers.equalTo(genericMap2)));
    }

    @Test
    public void basicGeneric() throws NotSerializableException {
        A a1 = new A<>(1);

        SerializerFactory factory = testDefaultFactory();

        SerializationOutput ser = new SerializationOutput(factory);
        SerializedBytes<?> bytes = ser.serialize(a1, TestSerializationContext.testSerializationContext);

        DeserializationInput des = new DeserializationInput(factory);
        A a2 = des.deserialize(bytes, A.class, TestSerializationContext.testSerializationContext);

        assertEquals(1, a2.getT());
    }

    private SerializedBytes<?> forceWildcardSerialize(A<?> a) throws NotSerializableException {
        SerializerFactory factory = testDefaultFactory();

        return (new SerializationOutput(factory)).serialize(a, TestSerializationContext.testSerializationContext);
    }

    private SerializedBytes<?> forceWildcardSerializeFactory(
            A<?> a,
            SerializerFactory factory) throws NotSerializableException {
        return (new SerializationOutput(factory)).serialize(a, TestSerializationContext.testSerializationContext);
    }

    private A<?> forceWildcardDeserialize(SerializedBytes<?> bytes) throws NotSerializableException {
        SerializerFactory factory = testDefaultFactory();

        DeserializationInput des = new DeserializationInput(factory);
        return des.deserialize(bytes, A.class, TestSerializationContext.testSerializationContext);
    }

    private A<?> forceWildcardDeserializeFactory(
            SerializedBytes<?> bytes,
            SerializerFactory factory) throws NotSerializableException {
        return (new DeserializationInput(factory)).deserialize(bytes, A.class,
                TestSerializationContext.testSerializationContext);
    }

    @Test
    public void forceWildcard() throws NotSerializableException {
        SerializedBytes<?> bytes = forceWildcardSerialize(new A<>(new Inner(29)));
        Inner i = (Inner) forceWildcardDeserialize(bytes).getT();
        assertEquals(29, i.getV());
    }

    @Test
    public void forceWildcardSharedFactory() throws NotSerializableException {
        SerializerFactory factory = testDefaultFactory();

        SerializedBytes<?> bytes = forceWildcardSerializeFactory(new A<>(new Inner(29)), factory);
        Inner i = (Inner) forceWildcardDeserializeFactory(bytes, factory).getT();

        assertEquals(29, i.getV());
    }
}
