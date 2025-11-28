package pub.carzy.auto_script.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class HeaderToolbarAdapter extends ArrayAdapter<String> {
    public HeaderToolbarAdapter(@NonNull Context context, @NonNull List<String> items) {
        super(context, 0, items);
    }

}
