package seng202.group8.datacontroller;


import seng202.group8.data.Data;

import java.util.ArrayList;

class DummyObserver<DummyData extends Data> implements DataObserver<DummyData> {
    public ArrayList<DummyData> dataChanged = new ArrayList<>();
    public ArrayList<Integer> dataDeleted = new ArrayList<>();

    @Override
    public void dataChangedEvent(DummyData data) {
        dataChanged.add(data);
    }
}
