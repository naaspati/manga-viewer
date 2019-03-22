package samrock;

public enum Views {
	VIEWELEMENTS_VIEW(3),
	
	CHAPTERS_LIST_VIEW(1), 
	DATA_VIEW(0), 
	CHAPTERS_EDIT_VIEW(2),
	
	NOTHING_FOUND_VIEW(4);
	
	private final int index;
    
    private Views(int index) {
        this.index = index;
    }
    public static Views parse(String str) {
        if(str == null) return null;
        if(str.matches("\\d+")) {
            int index = Integer.parseInt(str);
            for (Views v : Views.values()) {
                if(v.index == index)
                    return v;
            }
            return null;
        }
        return Views.valueOf(str);
    }
    public int index() {
        return index;
    }
}
