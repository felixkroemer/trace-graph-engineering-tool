package com.felixkroemer.trace_graph_engineering_tool.model.source_table;

import org.apache.commons.lang3.NotImplementedException;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.SUIDFactory;
import org.cytoscape.model.VirtualColumnInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class TraceGraphColumn implements CyColumn {

    private final TraceGraphSourceTable table;
    private final Long suid = SUIDFactory.getNextSUID();
    private String columnName;
    private double[] data;

    TraceGraphColumn(final TraceGraphSourceTable table, final String columnName, double[] data) {
        this.table = table;
        this.columnName = columnName;
        this.data = data;
    }

    @Override
    public Long getSUID() {
        return suid;
    }

    @Override
    public CyTable getTable() {
        return table;
    }

    @Override
    public String getName() {
        return columnName;
    }

    @Override
    public void setName(final String newName) {
        throw new IllegalArgumentException("can't rename column.");
    }

    @Override
    public Class<?> getType() {
        return Double.class;
    }

    @Override
    public Class<?> getListElementType() {
        throw new NotImplementedException("getListElementType is not implemented for TraceGraphColumn");
    }

    @Override
    public boolean isPrimaryKey() {
        return false;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public <T> List<T> getValues(final Class<? extends T> type) {
        if (type != Double.class) {
            throw new IllegalArgumentException("Only Double allowed");
        }
        return (List<T>) Arrays.stream(this.data).boxed().collect(Collectors.toList());
    }

    @Override
    public VirtualColumnInfo getVirtualColumnInfo() {
        return new VirtualColumnInfo() {
            @Override
            public boolean isVirtual() {
                return false;
            }

            @Override
            public String getSourceColumn() {
                return getName();
            }

            @Override
            public String getSourceJoinKey() {
                return null;
            }

            @Override
            public String getTargetJoinKey() {
                return null;
            }

            @Override
            public CyTable getSourceTable() {
                return getTable();
            }

            @Override
            public boolean isImmutable() {
                return false;
            }
        };
    }

    @Override
    public Object getDefaultValue() {
        return 0;
    }

    @Override
    public String toString() {
        return columnName;
    }
}
