package hw2;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by GleasonK on 2/13/16.
 */
public class MaxData implements Serializable {
//    public static final String TYPE_JOIN = "join";
    public static final String TYPE_RESULT = "result";
    public static final String TYPE_REQUEST = "request";

    public int row;
    public int[] vals;
    public String type;
    public int max;

    public MaxData(int row, int[] vals, String type){
        this.row=row;
        this.vals=vals;
        this.type=type;
    }

    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public String toString() {
        if (type.equals(TYPE_RESULT)){
            return String.format("{row: %d, type:%s, max:%s}", row, this.type, this.max);
        }
        return String.format("{row: %d, type:%s, vals:%s}", row, this.type, Arrays.toString(vals));
    }
}
