package com.felixkroemer.trace_graph_engineering_tool.model.source_table;

import org.apache.commons.lang3.NotImplementedException;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.events.TableTitleChangedEvent;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.*;

public class Trace implements CyTable {

    private final CyServiceRegistrar registrar;
    private final Long suid;
    private final Object lock = new Object();
    private String title;
    private Map<String, CyColumn> columns;
    private Map<String, double[]> data;
    private CyRow[] rows;
    private boolean isPublic;
    private int rowCount;

    public Trace(String title, long rowCount, CyServiceRegistrar registrar) {
        this.title = title;
        this.columns = new HashMap<>();
        this.registrar = registrar;
        this.data = new HashMap<>();
        this.suid = SUIDFactory.getNextSUID();
        this.isPublic = true;
        this.rowCount = (int) rowCount;
        this.rows = new CyRow[(int) rowCount];
        for (int i = 1; i <= rowCount; i++) {
            rows[i - 1] = new TraceRow(i);
        }
    }

    @Override
    public boolean isPublic() {
        return this.isPublic;
    }

    @Override
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public Mutability getMutability() {
        return Mutability.MUTABLE;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        boolean valueChanged;
        String oldTitle = null;
        synchronized (lock) {
            valueChanged = !this.title.equals(title);
            if (valueChanged) {
                oldTitle = this.title;
                this.title = title;
            }
        }
        if (valueChanged) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new TableTitleChangedEvent(this, oldTitle));
        }
    }

    @Override
    public CyColumn getPrimaryKey() {
        return new TracePrimaryColumn(this);
    }

    @Override
    public CyColumn getColumn(String fullyQualifiedName) {
        return this.columns.get(fullyQualifiedName);
    }

    @Override
    public Collection<CyColumn> getColumns() {
        return columns.values();
    }

    @Override
    public void deleteColumn(String fullyQualifiedName) {
        throw new NotImplementedException("deleteColumn is not implemented for Trace");
    }

    @Override
    public <T> void createColumn(String fullyQualifiedName, Class<? extends T> type, boolean isImmutable,
                                 T defaultValue) {
        var data = new double[rowCount + 1];
        this.data.put(fullyQualifiedName, data);
        this.columns.put(fullyQualifiedName, new TraceColumn(this, fullyQualifiedName, data));
    }

    @Override
    public <T> void createColumn(String fullyQualifiedName, Class<? extends T> type, boolean isImmutable) {
        createColumn(fullyQualifiedName, type, isImmutable, null);
    }

    @Override
    public <T> void createListColumn(String fullyQualifiedName, Class<T> listElementType, boolean isImmutable) {
        throw new NotImplementedException("createListColumn is not implemented for Trace");
    }

    @Override
    public <T> void createListColumn(String fullyQualifiedName, Class<T> listElementType, boolean isImmutable,
                                     List<T> defaultValue) {
        throw new NotImplementedException("createListColumn is not implemented for Trace");
    }

    @Override
    public CyRow getRow(Object primaryKey) {
        var index = ((Long) primaryKey).intValue();
        return this.rows[index - 1];
    }

    @Override
    public boolean rowExists(Object primaryKey) {
        int index = (int) primaryKey;
        return index > 0 && index <= rowCount;
    }

    @Override
    public boolean deleteRows(Collection<?> primaryKeys) {
        //throw new NotImplementedException("deleteRows is not implemented for Trace");
        return true;
    }

    @Override
    public List<CyRow> getAllRows() {
        return Arrays.asList(this.rows);
    }

    @Override
    public String getLastInternalError() {
        return "";
    }

    @Override
    public Collection<CyRow> getMatchingRows(String fullyQualifiedName, Object value) {
        throw new NotImplementedException("getMatchingRows is not implemented for Trace");
    }

    @Override
    public <T> Collection<T> getMatchingKeys(String fullyQualifiedName, Object value, Class<T> type) {
        throw new NotImplementedException("getMatchingKeys is not implemented for Trace");
    }

    @Override
    public int countMatchingRows(String fullyQualifiedName, Object value) {
        throw new NotImplementedException("countMatchingRows is not implemented for Trace");
    }

    @Override
    public int getRowCount() {
        return this.rowCount;
    }

    @Override
    public String addVirtualColumn(String virtualColumn, String sourceColumn, CyTable sourceTable, String targetJoinKey,
                                   boolean isImmutable) {
        throw new NotImplementedException("addVirtualColumn is not implemented for Trace");
    }

    @Override
    public void addVirtualColumns(CyTable sourceTable, String targetJoinKey, boolean isImmutable) {
        throw new NotImplementedException("addVirtualColumn is not implemented for Trace");
    }

    @Override
    public SavePolicy getSavePolicy() {
        return SavePolicy.DO_NOT_SAVE;
    }

    @Override
    public void setSavePolicy(SavePolicy policy) {
        throw new NotImplementedException("setSavePolicy is not implemented for Trace");
    }

    @Override
    public void swap(CyTable otherTable) {
        throw new NotImplementedException("swap is not implemented for Trace");
    }

    public double getValue(String name, long index) {
        return this.data.get(name)[(int) index];
    }

    @Override
    public Long getSUID() {
        return this.suid;
    }

    @Override
    public String toString() {
        return this.getTitle();
    }

    private final class TraceRow implements CyRow {

        private long index;
        private Long suid = SUIDFactory.getNextSUID();

        public TraceRow(long index) {
            this.index = index;
        }

        @Override
        public <T> T get(String fullyQualifiedName, Class<? extends T> type) {
            return get(fullyQualifiedName, type, null);
        }

        @Override
        public <T> T get(String fullyQualifiedName, Class<? extends T> type, T defaultValue) {
            if (fullyQualifiedName.equals(getPrimaryKey().getName())) {
                return (T) Long.valueOf(index);
            } else {
                return (T) Double.valueOf(getValue(fullyQualifiedName, index));
            }
        }

        @Override
        public <T> List<T> getList(String fullyQualifiedName, Class<T> listElementType) {
            throw new NotImplementedException("swap is not implemented for TraceRow");
        }

        @Override
        public <T> List<T> getList(String fullyQualifiedName, Class<T> listElementType, List<T> defaultValue) {
            throw new NotImplementedException("swap is not implemented for TraceRow");
        }

        @Override
        public <T> void set(String fullyQualifiedName, T value) {
            data.get(fullyQualifiedName)[(int) index] = (double) value;
        }

        @Override
        public boolean isSet(String fullyQualifiedName) {
            return true;
        }

        @Override
        public Map<String, Object> getAllValues() {
            final Map<String, Object> nameToValueMap = new HashMap<>(data.size());
            for (var entry : data.entrySet()) {
                nameToValueMap.put(entry.getKey(), entry.getValue()[(int) index]);
            }
            return nameToValueMap;
        }

        @Override
        public Object getRaw(String fullyQualifiedName) {
            return get(fullyQualifiedName, Double.class);
        }

        @Override
        public CyTable getTable() {
            return Trace.this;
        }

        @Override
        public Long getSUID() {
            return this.suid;
        }
    }
}
