package jeong_won_hyeok.inhatc.talktudy;

import android.graphics.drawable.Drawable;

public class ListViewItem {
    private Drawable icon;
    private String title;

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public Drawable getIcon() {
        return this.icon;
    }
    public String getTitle() {
        return this.title;
    }
}
