// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.persistence.typeHandling.coreTypes.factories;

import org.terasology.engine.persistence.typeHandling.TypeHandler;
import org.terasology.engine.persistence.typeHandling.TypeHandlerFactory;
import org.terasology.engine.persistence.typeHandling.TypeHandlerContext;
import org.terasology.engine.persistence.typeHandling.coreTypes.EnumTypeHandler;
import org.terasology.nui.reflection.TypeInfo;

import java.util.Optional;

/**
 * A {@link TypeHandlerFactory} that generates an {@link EnumTypeHandler} for enum types.
 */
public class EnumTypeHandlerFactory implements TypeHandlerFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<TypeHandler<T>> create(TypeInfo<T> typeInfo, TypeHandlerContext context) {
        Class<? super T> enumClass = typeInfo.getRawType();
        if (!Enum.class.isAssignableFrom(enumClass) || Enum.class.equals(enumClass)) {
            return Optional.empty();
        }

        while (!enumClass.isEnum()) {
            enumClass = enumClass.getSuperclass();
        }

        return Optional.of((TypeHandler<T>) new EnumTypeHandler(enumClass));
    }
}
