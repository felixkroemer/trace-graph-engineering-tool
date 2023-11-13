package com.felixkroemer.trace_graph_engineering_tool.model.source_table;

import org.apache.commons.lang3.NotImplementedException;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.SUIDFactory;
import org.cytoscape.model.VirtualColumnInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class TraceGraphPrimaryColumn implements CyColumn {
    private TraceGraphSourceTable table;
    private final Long suid = SUIDFactory.getNextSUID();

    public TraceGraphPrimaryColumn(TraceGraphSourceTable table) {
        this.table = table;
    }

    @Override
    public String getName() {
        return "id";
    }

    @Override
    public void setName(String fullyQualifiedName) {
        throw new NotImplementedException("setName is not implemented for TraceGraphPrimaryColumn");
    }

    @Override
    public Class<?> getType() {
        return Long.class;
    }

    @Override
    public Class<?> getListElementType() {
        throw new NotImplementedException("getListElementType is not implemented for TraceGraphPrimaryColumn");
    }

    @Override
    public boolean isPrimaryKey() {
        return true;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public CyTable getTable() {
        return this.table;
    }

    @Override
    public <T> List<T> getValues(Class<? extends T> type) {
        return (List<T>) LongStream.range(0, this.table.getRowCount()).boxed().collect(Collectors.toList());
    }

    @Override
    public VirtualColumnInfo getVirtualColumnInfo() {
        throw new NotImplementedException("getVirtualColumnInfo is not implemented for TraceGraphPrimaryColumn");
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public Long getSUID() {
        return this.suid;
    }
}
