package jeong_won_hyeok.inhatc.talktudy;

import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import static jeong_won_hyeok.inhatc.talktudy.MapsActivity.alarmList;
import static jeong_won_hyeok.inhatc.talktudy.MapsActivity.placeList;

public class AlertList extends AppCompatActivity {

    boolean isAllChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        // 빈 데이터 리스트 생성.
        final ArrayList<String> items = new ArrayList<String>();
        // ArrayAdapter 생성. 아이템 View를 선택(multiple choice)가능하도록 만듦.
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, items);

        // listview 생성 및 adapter 지정.
        final ListView listview = (ListView) findViewById(R.id.alert_listview);
        listview.setAdapter(adapter);

        // 데이터 담기
        for(String a : placeList) {
            items.add(a);
        }

        // 레이아웃 설정
        final Button selectAllButton = (Button)findViewById(R.id.alert_all) ;
        final Button deleteButton = (Button)findViewById(R.id.alert_del) ;
        final TextView close = (TextView)findViewById(R.id.alert_close);

        // 버튼 설정
        selectAllButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                int count = 0;
                count = adapter.getCount();

                if (!isAllChecked) {
                    for (int i=0; i<count; i++) {
                        listview.setItemChecked(i, true);
                    }
                    selectAllButton.setText("모두 선택");
                    isAllChecked = true;
                } else {
                    for (int i=0; i<count; i++) {
                        listview.setItemChecked(i, false);
                    }
                    selectAllButton.setText("모두 해제");
                    isAllChecked = false;
                }
            }
        }) ;

        deleteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                SparseBooleanArray checkedItems = listview.getCheckedItemPositions();
                int count = adapter.getCount();

                for (int i=count-1; i>=0; i--) {
                    if (checkedItems.get(i)) {
                        items.remove(i);
                        placeList.remove(i);
                        alarmList.remove(i);
                    }
                }

                // 모든 선택 상태 초기화.
                listview.clearChoices();
                adapter.notifyDataSetChanged();
            }
        }) ;

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
