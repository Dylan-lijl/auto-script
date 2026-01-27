package pub.carzy.auto_script.activities.about.child;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.popup.QMUIQuickAction;
import com.qmuiteam.qmui.widget.textview.QMUILinkTextView;

import java.util.Date;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutBasicInfoBinding;
import pub.carzy.auto_script.model.AboutBasicInfoModel;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.MixedUtil;

/**
 * @author admin
 */
public class AboutBasicInfoActivity extends BaseActivity {
    private ComAboutBasicInfoBinding binding;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.about_basic_info_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_basic_info);
        initTopBar();
        init();
    }
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
    private void init() {
        AboutBasicInfoModel model = new AboutBasicInfoModel();
        model.setMinSdk(ActivityUtils.minSdk(this));
        model.setTargetSdk(ActivityUtils.targetSdk(this));
        model.setCurrentSdk(ActivityUtils.currentApiVersion());
        model.setVersion(ActivityUtils.getVersionName(this));
        model.setSourceRepository(MixedUtil.githubSourceRepositoryUrl());
        model.setUpdateTime(MixedUtil.getReleaseTime());
        binding.setModel(model);
        binding.btnSourceRepository.setOnLinkClickListener(new QMUILinkTextView.OnLinkClickListener() {
            @Override
            public void onTelLinkClick(String phoneNumber) {

            }

            @Override
            public void onMailLinkClick(String mailAddress) {

            }

            @Override
            public void onWebUrlLinkClick(String url) {
                ActivityUtils.openToBrowser(getBaseContext(), binding.btnSourceRepository.getText().toString());
            }
        });
        binding.btnSourceRepository.setOnLinkLongClickListener(v -> {
            //打开悬浮层
            QMUIQuickAction action = ActivityUtils.createQuickAction(this);
            action.addAction(new QMUIQuickAction.Action()
                            .icon(ActivityUtils.getDrawable(this, R.drawable.copy, R.color.link))
                            .text(getString(R.string.copy))
                            .onClick(
                                    (quickAction, action1, position) -> {
                                        quickAction.dismiss();
                                        ActivityUtils.copyToClipboard(this, "link", binding.btnSourceRepository.getText().toString());
                                        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
                                    }
                            ))
                    .addAction(new QMUIQuickAction.Action()
                            .icon(ActivityUtils.getDrawable(this, R.drawable.browser, R.color.rainbow_orange))
                            .text(getString(R.string.open))
                            .onClick((quickAction, action1, position) -> {
                                        ActivityUtils.openToBrowser(getBaseContext(), binding.btnSourceRepository.getText().toString());
                                        quickAction.dismiss();
                                    }
                            ));
            action.show(binding.btnSourceRepository);
        });
    }

}
