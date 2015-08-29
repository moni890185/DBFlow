package com.raizlabs.android.dbflow.processor.definition;

import com.google.common.collect.Sets;
import com.raizlabs.android.dbflow.processor.definition.column.ColumnDefinition;
import com.raizlabs.android.dbflow.processor.model.ProcessorManager;
import com.raizlabs.android.dbflow.processor.utils.WriterUtils;
import com.raizlabs.android.dbflow.processor.writer.ExistenceWriter;
import com.raizlabs.android.dbflow.processor.writer.FlowWriter;
import com.raizlabs.android.dbflow.processor.writer.LoadCursorWriter;
import com.raizlabs.android.dbflow.processor.writer.SQLiteStatementWriter;
import com.raizlabs.android.dbflow.processor.definition.method.ToModelMethod;
import com.raizlabs.android.dbflow.processor.writer.WhereQueryWriter;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javawriter.JavaWriter;

import java.io.IOException;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Description: Used in writing model container adapters
 */
public class ModelContainerDefinition extends BaseDefinition {

    public static final String DBFLOW_MODEL_CONTAINER_TAG = "Container";

    private FlowWriter[] methodWriters;
    private TableDefinition tableDefinition;

    public ModelContainerDefinition(TypeElement classElement, ProcessorManager manager) {
        super(classElement, manager);
        tableDefinition = manager.getTableDefinition(manager.getDatabase(elementClassName), classElement);

        setOutputClassName(tableDefinition.databaseMethod.classSeparator + DBFLOW_MODEL_CONTAINER_TAG);

        methodWriters = new FlowWriter[]{
                new SQLiteStatementWriter(tableDefinition, true, tableDefinition.implementsSqlStatementListener,
                                          tableDefinition.implementsContentValuesListener),
                new ExistenceWriter(tableDefinition, true),
                new WhereQueryWriter(tableDefinition, true),
                new ToModelMethod(tableDefinition, true),
                new LoadCursorWriter(tableDefinition, true, tableDefinition.implementsLoadFromCursorListener)
        };
    }

    @Override
    protected String getExtendsClass() {
        return "ModelContainerAdapter<" + elementClassName + ">";
    }

    @Override
    public void onWriteDefinition(TypeSpec.Builder typeBuilder) {

        javaWriter.emitField("Map<String, Class<?>>", "columnMap", Sets.newHashSet(Modifier.PRIVATE, Modifier.FINAL),
                             "new HashMap<>()");
        javaWriter.emitEmptyLine();

        javaWriter.beginConstructor(Sets.newHashSet(Modifier.PUBLIC));

        for (ColumnDefinition columnDefinition : tableDefinition.columnDefinitions) {
            javaWriter.emitStatement("%1s.put(\"%1s\", %1s.class)", "columnMap", columnDefinition.columnName,
                                     columnDefinition.columnFieldType);
        }

        javaWriter.endConstructor();

        javaWriter.emitEmptyLine();
        javaWriter.emitAnnotation(Override.class);
        WriterUtils.emitMethod(javaWriter, new FlowWriter() {
            @Override
            public void write(JavaWriter javaWriter) throws IOException {
                javaWriter.emitStatement("return %1s.get(%1s)", "columnMap", "columnName");
            }
        }, "Class<?>", "getClassForColumn", Sets.newHashSet(Modifier.PUBLIC, Modifier.FINAL), "String", "columnName");

        InternalAdapterHelper.writeGetModelClass(javaWriter, getModelClassQualifiedName());
        InternalAdapterHelper.writeGetTableName(javaWriter,
                                                elementClassName + tableDefinition.databaseMethod.classSeparator +
                                                TableDefinition.DBFLOW_TABLE_TAG);

        for (FlowWriter writer : methodWriters) {
            writer.write(javaWriter);
        }
    }

    public String getModelClassQualifiedName() {
        return ((TypeElement) element).getQualifiedName().toString();
    }
}
