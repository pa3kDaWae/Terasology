// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.entitySystem.metadata;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.Component;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.engine.network.Replicate;
import org.terasology.nui.reflection.copy.CopyStrategy;
import org.terasology.nui.reflection.copy.CopyStrategyLibrary;
import org.terasology.nui.reflection.metadata.ClassMetadata;
import org.terasology.nui.reflection.reflect.InaccessibleFieldException;
import org.terasology.nui.reflection.reflect.ReflectFactory;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.engine.world.block.RequiresBlockLifecycleEvents;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Metadata on a component class and its fields.
 *
 */
public class ComponentMetadata<T extends Component> extends ClassMetadata<T, ComponentFieldMetadata<T, ?>> {

    private boolean replicated;
    private boolean replicatedFromOwner;
    private boolean referenceOwner;
    private boolean forceBlockActive;
    private boolean retainUnalteredOnBlockChange;
    private final boolean blockLifecycleEventsRequired;
    private final List<Annotation> annotations;

    /**
     * @param uri            The uri to identify the component with.
     * @param type           The type to create the metadata for
     * @param factory        A reflection library to provide class construction and field get/set functionality
     * @param copyStrategies A copy strategy library
     * @throws NoSuchMethodException If the component has no default constructor
     */
    public ComponentMetadata(ResourceUrn uri, Class<T> type, ReflectFactory factory, CopyStrategyLibrary copyStrategies) throws NoSuchMethodException {
        super(uri, type, factory, copyStrategies, Predicates.alwaysTrue());
        replicated = type.getAnnotation(Replicate.class) != null;
        blockLifecycleEventsRequired = type.getAnnotation(RequiresBlockLifecycleEvents.class) != null;
        ForceBlockActive forceBlockActiveAnnotation = type.getAnnotation(ForceBlockActive.class);
        if (forceBlockActiveAnnotation != null) {
            forceBlockActive = true;
            retainUnalteredOnBlockChange = forceBlockActiveAnnotation.retainUnalteredOnBlockChange();
        }

        for (ComponentFieldMetadata<T, ?> field : getFields()) {
            if (field.isReplicated()) {
                replicated = true;
                if (field.getReplicationInfo().value().isReplicateFromOwner()) {
                    replicatedFromOwner = true;
                }
            }
            if (field.isOwnedReference()) {
                referenceOwner = true;
            }
        }

        annotations = Lists.newArrayList(type.getAnnotations());
    }

    @Override
    protected <V> ComponentFieldMetadata<T, V> createField(Field field, CopyStrategy<V> copyStrategy, ReflectFactory factory) throws InaccessibleFieldException {
        return new ComponentFieldMetadata<>(this, field, copyStrategy, factory, false);
    }

    /**
     * @return Whether this component owns any references
     */
    public boolean isReferenceOwner() {
        return referenceOwner;
    }

    /**
     * @return Whether this component replicates any fields from owner to server
     */
    public boolean isReplicatedFromOwner() {
        return replicatedFromOwner;
    }

    /**
     * @return Whether this component needs to be replicated
     */
    public boolean isReplicated() {
        return replicated;
    }

    /**
     * @return Whether this component forces a block active
     */
    public boolean isForceBlockActive() {
        return forceBlockActive;
    }

    /**
     * @return Whether this component should be retained unaltered on block change
     */
    public boolean isRetainUnalteredOnBlockChange() {
        return retainUnalteredOnBlockChange;
    }

    /**
     * @return Whether this component makes a block valid for block lifecycle events
     */
    public boolean isBlockLifecycleEventsRequired() {
        return blockLifecycleEventsRequired;
    }

    public T getAnnotation(final Class<T> type) {
        return Iterables.getOnlyElement(Iterables.filter(annotations, type), null);
    }

    /**
     * Makes a copy, and if there are any entities owned by this component, copy those too recursively.
     *
     * @param object The instance of this class to copy
     * @return A copy of the given object
     */
    public T copyWithOwnedEntities(T object) {
        T result = newInstance();
        if (result != null) {
            for (ComponentFieldMetadata<T,?> field : getFields()) {
                field.setValue(result, field.getCopyOfValueWithOwnedEntities(object));
            }
        }
        return result;
    }

    /**
     * This method is for use in situations where metadata is being used generically and the actual type of the value cannot be
     *
     * @param object The instance of this class to copy
     * @return A copy of the given object, or null if object is not of the type described by this metadata.
     */
    public T copyWithOwnedEntitiesRaw(Object object) {
        if (getType().isInstance(object)) {
            return copyWithOwnedEntities(getType().cast(object));
        }
        return null;
    }
}
