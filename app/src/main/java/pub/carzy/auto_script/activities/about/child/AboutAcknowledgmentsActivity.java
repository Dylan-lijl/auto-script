package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutAcknowledgmentsBinding;
import pub.carzy.auto_script.entity.AcknowledgementEntity;
import pub.carzy.auto_script.entity.AcknowledgementImport;
import pub.carzy.auto_script.model.AboutAcknowledgmentModel;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutAcknowledgmentsActivity extends BaseActivity {
    private ComAboutAcknowledgmentsBinding binding;
    private AboutAcknowledgmentModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_acknowledgments);
        model = new AboutAcknowledgmentModel();
        binding.setModel(model);
        loadData();
    }

    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            InputStream is = getResources().openRawResource(R.raw.acknowledgements);
            try {
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    Gson gson = new Gson();
                    AcknowledgementImport data = gson.fromJson(reader, AcknowledgementImport.class);
                    data.getData().sort(Comparator.comparing(AcknowledgementEntity::getOrder));
                    model.setData(data.getData());
                    updateList();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void updateList() {
        ThreadUtil.runOnUi(()->{
            //移除旧数据
            binding.acknowledgmentList.removeAllViews();
            //从小新建
            for (AcknowledgementEntity item:model.getData()){
                QMUICommonListItemView view = binding.acknowledgmentList.createItemView(item.getTitle());
                view.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
                ImageView imageView = new ImageView(this);
                if (item.getType() == AcknowledgementEntity.PEOPLE){
                    imageView.setImageDrawable(ActivityUtils.getDrawable(this,R.drawable.user,R.color.black));
                }
                view.addAccessoryCustomView(imageView);
                //todo
            }
        });
    }
}
