package com.example.aplicatiegestiunefirma.network;

import com.example.aplicatiegestiunefirma.model.ChatMessage;
import com.example.aplicatiegestiunefirma.model.Company;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.example.aplicatiegestiunefirma.model.Leave;
import com.example.aplicatiegestiunefirma.model.Project;
import com.example.aplicatiegestiunefirma.model.WorkSchedule;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/register")
    Call<ResponseBody> register(@Body RegisterRequest request);

    // File upload (poze rapoarte, contracte PDF, facturi PDF)
    @Multipart
    @POST("/upload")
    Call<UploadResponse> uploadFile(@Header("Authorization") String token, @Part MultipartBody.Part file);

    // Company
    @GET("/company")
    Call<Company> getCompany(@Header("Authorization") String token);

    @POST("/company")
    Call<ResponseBody> saveCompany(@Header("Authorization") String token, @Body Company company);

    // Employees
    @GET("/angajati")
    Call<List<Employee>> getEmployees(@Header("Authorization") String token);

    @POST("/angajati")
    Call<ResponseBody> addEmployee(@Header("Authorization") String token, @Body Employee employee);

    @PUT("/employees/{id}")
    Call<ResponseBody> updateEmployee(@Header("Authorization") String token, @Path("id") int id, @Body Employee employee);

    @DELETE("/employees/{id}")
    Call<ResponseBody> deleteEmployee(@Header("Authorization") String token, @Path("id") int id);

    // Employee Accounts (created by admin)
    @POST("/employees/{id}/create-account")
    Call<ResponseBody> createEmployeeAccount(@Header("Authorization") String token, @Path("id") int employeeId, @Body CreateEmployeeAccountRequest request);

    @GET("/employees/{id}/has-account")
    Call<ResponseBody> checkEmployeeAccount(@Header("Authorization") String token, @Path("id") int employeeId);

    // Projects
    @GET("/proiecte")
    Call<List<Project>> getProjects(@Header("Authorization") String token);

    @POST("/proiecte")
    Call<ResponseBody> addProject(@Header("Authorization") String token, @Body Project project);

    @PUT("/projects/{id}")
    Call<ResponseBody> updateProject(@Header("Authorization") String token, @Path("id") int id, @Body Project project);

    @DELETE("/projects/{id}")
    Call<ResponseBody> deleteProject(@Header("Authorization") String token, @Path("id") int id);

    // Invoices
    @GET("/facturi")
    Call<List<Invoice>> getInvoices(@Header("Authorization") String token);

    @POST("/facturi")
    Call<ResponseBody> addInvoice(@Header("Authorization") String token, @Body Invoice invoice);

    @PUT("/invoices/{id}")
    Call<ResponseBody> updateInvoice(@Header("Authorization") String token, @Path("id") int id, @Body Invoice invoice);

    @DELETE("/invoices/{id}")
    Call<ResponseBody> deleteInvoice(@Header("Authorization") String token, @Path("id") int id);

    // Reports (admin)
    @GET("/rapoarte_zilnice")
    Call<List<DailyReport>> getReports(@Header("Authorization") String token, @Query("date") String date);

    @POST("/rapoarte_zilnice")
    Call<ResponseBody> addReport(@Header("Authorization") String token, @Body DailyReport report);

    @PUT("/reports/{id}")
    Call<ResponseBody> updateReport(@Header("Authorization") String token, @Path("id") int id, @Body DailyReport report);

    @DELETE("/reports/{id}")
    Call<ResponseBody> deleteReport(@Header("Authorization") String token, @Path("id") int id);

    // Employee Portal (employee-role endpoints)
    @GET("/employee/info")
    Call<Employee> getEmployeeInfo(@Header("Authorization") String token);

    @GET("/employee/rapoarte")
    Call<List<DailyReport>> getEmployeeOwnReports(@Header("Authorization") String token);

    @POST("/employee/rapoarte")
    Call<ResponseBody> addEmployeeOwnReport(@Header("Authorization") String token, @Body DailyReport report);

    @GET("/employee/proiecte")
    Call<List<Project>> getEmployeeProjects(@Header("Authorization") String token);

    // Leave (concedii)
    @GET("/concedii")
    Call<List<Leave>> getLeaves(@Header("Authorization") String token);

    @POST("/concedii")
    Call<ResponseBody> addLeave(@Header("Authorization") String token, @Body Leave leave);

    @PUT("/concedii/{id}")
    Call<ResponseBody> updateLeave(@Header("Authorization") String token, @Path("id") int id, @Body Leave leave);

    @DELETE("/concedii/{id}")
    Call<ResponseBody> deleteLeave(@Header("Authorization") String token, @Path("id") int id);

    // Work Schedule (program lucru)
    @GET("/program_lucru")
    Call<List<WorkSchedule>> getWorkSchedules(@Header("Authorization") String token);

    @POST("/program_lucru")
    Call<ResponseBody> addWorkSchedule(@Header("Authorization") String token, @Body WorkSchedule schedule);

    @DELETE("/program_lucru/{id}")
    Call<ResponseBody> deleteWorkSchedule(@Header("Authorization") String token, @Path("id") int id);

    // Chat
    @GET("/messages/{employeeId}")
    Call<List<ChatMessage>> getMessages(@Header("Authorization") String token, @Path("employeeId") int employeeId);

    @POST("/messages")
    Call<ResponseBody> sendMessage(@Header("Authorization") String token, @Body ChatMessageRequest request);

    @POST("/messages/{employeeId}/read")
    Call<ResponseBody> markMessagesRead(@Header("Authorization") String token, @Path("employeeId") int employeeId);

    // Inner Classes ─────────────────────────────────────────────────────────

    class LoginRequest {
        @SerializedName("email")
        public String email;
        @SerializedName("password")
        public String password;
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class RegisterRequest {
        @SerializedName("email")
        public String email;
        @SerializedName("password")
        public String password;
        @SerializedName("company_name")
        public String company_name;
        public RegisterRequest(String email, String password, String companyName) {
            this.email = email;
            this.password = password;
            this.company_name = companyName;
        }
    }

    class CreateEmployeeAccountRequest {
        @SerializedName("email")
        public String email;
        @SerializedName("password")
        public String password;
        public CreateEmployeeAccountRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class LoginResponse {
        @SerializedName("access_token")
        public String access_token;
        @SerializedName("token_type")
        public String token_type;
        @SerializedName("firma_id")
        public int firma_id;
        /** "admin" or "employee" */
        @SerializedName("role")
        public String role;
        /** null for admin accounts, employee DB id for employee accounts */
        @SerializedName("employee_id") public Integer employee_id;
    }

    class UploadResponse {
        @SerializedName("url")
        public String url;
    }

    class ChatMessageRequest {
        @SerializedName("employee_id") public int employeeId;
        @SerializedName("message") public String message;
        @SerializedName("is_from_admin") public boolean isFromAdmin;
        public ChatMessageRequest(int employeeId, String message, boolean isFromAdmin) {
            this.employeeId = employeeId;
            this.message = message;
            this.isFromAdmin = isFromAdmin;
        }
    }
}
