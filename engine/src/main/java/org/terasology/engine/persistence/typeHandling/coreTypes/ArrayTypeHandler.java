// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.persistence.typeHandling.coreTypes;

import com.google.common.collect.Lists;
import org.terasology.engine.persistence.typeHandling.PersistedData;
import org.terasology.engine.persistence.typeHandling.PersistedDataSerializer;
import org.terasology.engine.persistence.typeHandling.TypeHandler;
import org.terasology.nui.reflection.TypeInfo;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serializes arrays of type {@code E[]}.
 *
 * {@link ArrayTypeHandler} extends {@link TypeHandler<Object>} because the type parameter {@link E}
 * supports only wrapper types, and primitive array to wrapper type array (and vice versa) casts are
 * unsupported. The array is accessed via the {@link Array} utility class as an {@link Object} so that
 * the cast can be avoided.
 *
 * @param <E> The type of an element in the array to serialize.
 */
public class ArrayTypeHandler<E> extends TypeHandler<Object> {
    private final TypeHandler<E> elementTypeHandler;
    private final TypeInfo<E> elementType;

    public ArrayTypeHandler(TypeHandler<E> elementTypeHandler, TypeInfo<E> elementType) {
        this.elementTypeHandler = elementTypeHandler;
        this.elementType = elementType;
    }

    @Override
    protected PersistedData serializeNonNull(Object value, PersistedDataSerializer serializer) {
        List<PersistedData> items = Lists.newArrayList();

        for (int i = 0; i < Array.getLength(value); i++) {
            E element = (E) Array.get(value, i);
            items.add(elementTypeHandler.serialize(element, serializer));
        }

        return serializer.serialize(items);
    }

    @Override
    public Optional<Object> deserialize(PersistedData data) {
        if (!data.isArray()) {
            return Optional.empty();
        }

        @SuppressWarnings({"unchecked"})
        List<E> items = data.getAsArray().getAsValueArray().stream()
                .map(itemData -> elementTypeHandler.deserialize(itemData))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());

        Object array = Array.newInstance(elementType.getRawType(), items.size());

        for (int i = 0; i < items.size(); i++) {
            Array.set(array, i, items.get(i));
        }

        return Optional.of(array);
    }
}
