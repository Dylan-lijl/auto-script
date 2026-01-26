package pub.carzy.auto_script.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class SingleSimpleAdapter extends BaseAdapter {
    private final List<Data> dataList;

    public SingleSimpleAdapter(List<Data> dataList) {
        this.dataList = dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Data data = dataList.get(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.common_single_simple, parent, false);
            holder = new ViewHolder();
            holder.textBtn = convertView.findViewById(R.id.text_btn);
            holder.selectImg = convertView.findViewById(R.id.select_img);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textBtn.setText(data.text);
        holder.selectImg.setVisibility(data.selected ? View.VISIBLE : View.INVISIBLE);
        return convertView;
    }

    public static class Data {
        public String text;
        public boolean selected;
        public Data(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
        }
    }
    static class ViewHolder {
        TextView textBtn;
        ImageView selectImg;
    }
}
