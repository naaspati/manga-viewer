package samrock.utils;

import java.lang.ref.Reference;
import java.util.Map;

import sam.reference.ReferenceMap;
import sam.reference.ReferenceType;

public class SoftMap<K, V> extends ReferenceMap<K, V> {

    public SoftMap(Map<K, Reference<V>> map) {
        super(ReferenceType.SOFT, map);
    }
    public SoftMap() {
        super(ReferenceType.SOFT);
    }
}
