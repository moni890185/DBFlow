package com.raizlabs.android.dbflow.processor.definition.method;

import com.raizlabs.android.dbflow.processor.definition.OneToManyDefinition;
import com.raizlabs.android.dbflow.processor.definition.TableDefinition;
import com.raizlabs.android.dbflow.processor.utils.ModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

/**
 * Description: Overrides the save, update, and insert methods if the {@link com.raizlabs.android.dbflow.annotation.OneToMany.Method#SAVE} is used.
 */
public class OneToManySaveMethod implements MethodDefinition {
    public static final String METHOD_SAVE = "save";
    public static final String METHOD_UPDATE = "update";
    public static final String METHOD_INSERT = "insert";

    private final TableDefinition tableDefinition;
    private final boolean isModelContainerAdapter;
    private final String methodName;

    public OneToManySaveMethod(TableDefinition tableDefinition, boolean isModelContainerAdapter, String methodName) {
        this.tableDefinition = tableDefinition;
        this.isModelContainerAdapter = isModelContainerAdapter;
        this.methodName = methodName;
    }

    @Override
    public MethodSpec getMethodSpec() {
        CodeBlock.Builder code = CodeBlock.builder();
        for (OneToManyDefinition oneToManyDefinition : tableDefinition.oneToManyDefinitions) {
            oneToManyDefinition.writeSave(code);
        }
        code.addStatement("super.$L($L)", methodName, ModelUtils.getVariable(isModelContainerAdapter));

        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(tableDefinition.elementClassName, ModelUtils.getVariable(isModelContainerAdapter))
                .addCode(code.build())
                .build();
    }
}
