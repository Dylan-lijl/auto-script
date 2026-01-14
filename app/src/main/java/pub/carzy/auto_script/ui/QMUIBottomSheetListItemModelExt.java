package pub.carzy.auto_script.ui;

import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetListItemModel;

import java.lang.reflect.Field;

/**
 * @author admin
 */
public class QMUIBottomSheetListItemModelExt extends QMUIBottomSheetListItemModel {
    public QMUIBottomSheetListItemModelExt(CharSequence text, String tag) {
        super(text, tag);
    }
    public static Field textField;
    public void setText(CharSequence text) {
        if (textField==null){
            synchronized (QMUIBottomSheetListItemModelExt.class){
                try {
                    textField = QMUIBottomSheetListItemModel.class.getDeclaredField("text");
                    textField.setAccessible(true);
                } catch (Exception ignore) {
                }
            }
        }
        try {
            textField.set(this, text);
        } catch (IllegalAccessException ignored) {
        }
    }
}
