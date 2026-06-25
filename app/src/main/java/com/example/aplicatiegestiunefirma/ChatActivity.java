package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.ChatMessage;
import com.example.aplicatiegestiunefirma.network.ApiService;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private EditText etMessage;
    private ImageButton btnSend;
    private int employeeId;
    private String employeeName;
    private boolean isAdmin;
    private SharedPreferences sharedPreferences;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        employeeId = getIntent().getIntExtra("employee_id", -1);
        employeeName = getIntent().getStringExtra("employee_name");
        isAdmin = getIntent().getBooleanExtra("is_admin", true);

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSendMessage);

        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        tvChatTitle.setText("Chat cu: " + (employeeName != null ? employeeName : "Angajat"));

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        adapter = new ChatAdapter(new ArrayList<>(), isAdmin);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
        startPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshRunnable != null) refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void startPolling() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                refreshHandler.postDelayed(this, 5000); // refresh every 5s
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    private void loadMessages() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getMessages(token, employeeId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessage> messages = response.body();
                    runOnUiThread(() -> {
                        adapter.setData(messages);
                        if (!messages.isEmpty())
                            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    });
                }
            }
            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {}
        });
        // Mark messages as read
        RetrofitClient.getApiService().markMessagesRead(token, employeeId).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> r) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String token = "Bearer " + sharedPreferences.getString("token", "");
        ApiService.ChatMessageRequest req = new ApiService.ChatMessageRequest(employeeId, text, isAdmin);
        RetrofitClient.getApiService().sendMessage(token, req).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> etMessage.setText(""));
                    loadMessages();
                } else {
                    Toast.makeText(ChatActivity.this, "Eroare la trimitere", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Eroare de re\u021bea", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== ADAPTER ====================
    static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        List<ChatMessage> list;
        boolean isAdmin;
        ChatAdapter(List<ChatMessage> list, boolean isAdmin) { this.list = list; this.isAdmin = isAdmin; }
        void setData(List<ChatMessage> d) { list = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == 1 ? R.layout.item_chat_message_right : R.layout.item_chat_message_left;
            return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = list.get(position);
            return (isAdmin == msg.isFromAdmin()) ? 1 : 0;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ChatMessage msg = list.get(position);
            holder.tvMsg.setText(msg.getMessage());
            String ts = msg.getTimestamp();
            holder.tvTime.setText(ts != null && ts.length() > 16 ? ts.substring(11, 16) : ts);
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvMsg, tvTime;
            VH(View v) { super(v);
                tvMsg = v.findViewById(R.id.tvChatMessage);
                tvTime = v.findViewById(R.id.tvChatTime);
            }
        }
    }
}
