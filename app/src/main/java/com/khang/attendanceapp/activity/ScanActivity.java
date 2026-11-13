package com.khang.attendanceapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.model.Student;
import com.khang.attendanceapp.utils.AppPrefs;
import com.khang.attendanceapp.utils.AvatarUtils;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.FeedbackUtils;
import com.khang.attendanceapp.utils.TextUtils;
import com.khang.attendanceapp.view.ScanOverlayView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quét mã vạch trên thẻ sinh viên bằng CameraX + ML Kit.
 * Mã vạch = MSSV -> tra trong danh sách sinh viên -> ghi điểm danh vào SQLite.
 * Có thêm: bật đèn flash, nhập MSSV thủ công, quét từ ảnh trong thư viện.
 */
public class ScanActivity extends BaseActivity {

    private PreviewView previewView;
    private ScanOverlayView overlay;
    private View bottomPanel, permissionLayout;
    private MaterialCardView resultCard;
    private ImageButton btnFlash;
    private TextView tvScanCount;

    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private Camera camera;
    private boolean torchOn = false;

    /** true = đang chờ mã vạch; false = đang xử lý / hiển thị kết quả. */
    private final AtomicBoolean scanning = new AtomicBoolean(true);

    private DatabaseHelper db;
    private AppPrefs prefs;
    private int scannedCount = 0;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        db = DatabaseHelper.getInstance(this);
        prefs = new AppPrefs(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build());

        previewView = findViewById(R.id.previewView);
        overlay = findViewById(R.id.scanOverlay);
        bottomPanel = findViewById(R.id.bottomPanel);
        permissionLayout = findViewById(R.id.permissionLayout);
        resultCard = findViewById(R.id.resultCard);
        btnFlash = findViewById(R.id.btnFlash);
        tvScanCount = findViewById(R.id.tvScanCount);

        applyTopInset(findViewById(R.id.topBar));
        applyBottomInset(bottomPanel);
        applyBottomInset(findViewById(R.id.permissionLayout));
        updateCount();

        // Launcher xin quyền camera
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        showPermissionUi(false);
                        startCamera();
                    } else {
                        showPermissionUi(true);
                    }
                });

        // Launcher chọn ảnh từ thư viện (Intent ngầm định ACTION_GET_CONTENT)
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::scanFromImage);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPermissionBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnManual).setOnClickListener(v -> showManualDialog());
        findViewById(R.id.btnPermissionManual).setOnClickListener(v -> showManualDialog());
        findViewById(R.id.btnGallery).setOnClickListener(v -> imagePicker.launch("image/*"));
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
        findViewById(R.id.btnScanNext).setOnClickListener(v -> resumeScanning());
        btnFlash.setOnClickListener(v -> toggleTorch());
        findViewById(R.id.btnGrantPermission).setOnClickListener(v -> {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                // Đã chọn "Không hỏi lại" -> mở trang cài đặt ứng dụng
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                startActivity(i);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Người dùng có thể vừa cấp quyền trong Cài đặt
        if (permissionLayout.getVisibility() == View.VISIBLE
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showPermissionUi(false);
            startCamera();
        }
    }

    // ============================== CAMERA ==============================

    private void showPermissionUi(boolean show) {
        permissionLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                bindCamera(future.get());
            } catch (Exception e) {
                Toast.makeText(this, R.string.scan_camera_error, Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Độ phân giải 1280x720 giúp đọc mã vạch 1D (Code128/Code39...) rõ hơn
        ResolutionSelector selector = new ResolutionSelector.Builder()
                .setResolutionStrategy(new ResolutionStrategy(new Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build();
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setResolutionSelector(selector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyze);

        provider.unbindAll();
        camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
        btnFlash.setVisibility(camera.getCameraInfo().hasFlashUnit() ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy proxy) {
        if (!scanning.get()) {
            proxy.close();
            return;
        }
        Image media = proxy.getImage();
        if (media == null) {
            proxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(media, proxy.getImageInfo().getRotationDegrees());
        scanner.process(image)
                .addOnSuccessListener(barcodes -> handleBarcodes(barcodes, true))
                .addOnCompleteListener(task -> proxy.close());
    }

    private void handleBarcodes(List<Barcode> barcodes, boolean fromCamera) {
        for (Barcode b : barcodes) {
            String raw = b.getRawValue();
            if (raw == null || raw.trim().isEmpty()) continue;
            final String code = raw.trim();
            if (fromCamera) {
                // Chỉ nhận 1 mã mỗi lần: đóng cờ "đang quét" ngay
                if (!scanning.compareAndSet(true, false)) return;
            } else {
                scanning.set(false);
            }
            runOnUiThread(() -> onCodeDetected(code, true));
            return;
        }
        if (!fromCamera) {
            Toast.makeText(this, R.string.scan_gallery_none, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleTorch() {
        if (camera == null) return;
        torchOn = !torchOn;
        camera.getCameraControl().enableTorch(torchOn);
        btnFlash.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, torchOn ? R.color.accent : R.color.white)));
    }

    // ============================== XỬ LÝ MÃ VẠCH ==============================

    private void onCodeDetected(String code, boolean withFeedback) {
        if (withFeedback) {
            if (prefs.isSoundOn()) FeedbackUtils.beep();
            if (prefs.isVibrateOn()) FeedbackUtils.vibrate(this, 80);
        }
        scanning.set(false);
        Student s = db.getStudentByCode(code);
        if (s == null) {
            showNewStudentDialog(code);
        } else {
            recordAttendance(code, s.name, s.className);
        }
    }

    private void recordAttendance(String code, String name, String className) {
        String session = prefs.getSessionName();
        AttendanceRecord existing = db.findToday(code, session);
        if (existing != null) {
            showResult(existing, true);
            return;
        }
        AttendanceRecord r = new AttendanceRecord();
        r.studentCode = code;
        r.studentName = name;
        r.className = className;
        r.sessionName = session;
        r.timestamp = System.currentTimeMillis();
        r.status = computeStatus(r.timestamp);
        db.insertAttendance(r);
        scannedCount++;
        updateCount();
        showResult(r, false);
    }

    /** Đi trễ nếu bật tính năng và quá giờ bắt đầu + 15 phút. */
    private String computeStatus(long now) {
        if (!prefs.isLateEnabled()) return AttendanceRecord.STATUS_PRESENT;
        int start = prefs.getStartHour() * 60 + prefs.getStartMinute();
        return DateUtils.minutesOfDay(now) > start + 15
                ? AttendanceRecord.STATUS_LATE : AttendanceRecord.STATUS_PRESENT;
    }

    private void showResult(AttendanceRecord r, boolean duplicate) {
        TextView title = findViewById(R.id.tvResultTitle);
        ImageView icon = findViewById(R.id.imgResultIcon);
        int color = ContextCompat.getColor(this, duplicate ? R.color.warning : R.color.success);
        title.setText(duplicate ? R.string.scan_duplicate : R.string.scan_success);
        title.setTextColor(color);
        icon.setImageTintList(ColorStateList.valueOf(color));

        AvatarUtils.bind((TextView) findViewById(R.id.tvResultAvatar), r.studentName, r.studentCode);
        ((TextView) findViewById(R.id.tvResultName)).setText(r.studentName);
        String cls = TextUtils.isBlank(r.className) ? "" : " • " + r.className;
        ((TextView) findViewById(R.id.tvResultSub)).setText(r.studentCode + cls);
        ((TextView) findViewById(R.id.tvResultTime)).setText(DateUtils.formatTime(r.timestamp));

        TextView status = findViewById(R.id.tvResultStatus);
        status.setText(AttendanceRecord.statusLabel(r.status));
        status.setTextColor(ContextCompat.getColor(this, AttendanceRecord.statusColor(r.status)));
        status.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, AttendanceRecord.statusBgColor(r.status))));

        overlay.setFrameColor(ContextCompat.getColor(this, duplicate ? R.color.scan_duplicate : R.color.scan_success));
        overlay.setLineVisible(false);
        bottomPanel.setVisibility(View.GONE);

        // Thẻ kết quả trượt lên từ dưới
        resultCard.setVisibility(View.VISIBLE);
        resultCard.setAlpha(0f);
        resultCard.setTranslationY(160 * getResources().getDisplayMetrics().density);
        resultCard.animate().alpha(1f).translationY(0f).setDuration(320).start();
    }

    private void resumeScanning() {
        resultCard.setVisibility(View.GONE);
        bottomPanel.setVisibility(View.VISIBLE);
        overlay.setFrameColor(ContextCompat.getColor(this, R.color.scan_idle));
        overlay.setLineVisible(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            showPermissionUi(true);
        }
        scanning.set(true);
    }

    private void updateCount() {
        tvScanCount.setText(getString(R.string.scan_count_fmt, scannedCount));
    }

    // ============================== NHẬP TAY / ẢNH ==============================

    private void showManualDialog() {
        scanning.set(false);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_input, null);
        final TextInputLayout til = v.findViewById(R.id.tilInput);
        final TextInputEditText et = v.findViewById(R.id.etInput);
        til.setHint(getString(R.string.hint_student_code));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.scan_manual_title)
                .setView(v)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok, null)
                .setOnDismissListener(d -> {
                    if (resultCard.getVisibility() != View.VISIBLE) scanning.set(true);
                })
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String code = et.getText() == null ? "" : et.getText().toString().trim();
            if (code.isEmpty()) {
                til.setError(getString(R.string.error_required));
                return;
            }
            showPermissionUi(false);
            dialog.dismiss();
            onCodeDetected(code, false);
        });
    }

    private void scanFromImage(@Nullable Uri uri) {
        if (uri == null) return;
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> handleBarcodes(barcodes, false))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, R.string.scan_gallery_none, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Toast.makeText(this, R.string.scan_gallery_none, Toast.LENGTH_LONG).show();
        }
    }

    /** MSSV chưa có trong danh sách: cho nhập nhanh tên + lớp rồi điểm danh luôn. */
    private void showNewStudentDialog(final String code) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quick_student, null);
        TextView tvCode = v.findViewById(R.id.tvQuickCode);
        final TextInputLayout tilName = v.findViewById(R.id.tilQuickName);
        final TextInputEditText etName = v.findViewById(R.id.etQuickName);
        final TextInputEditText etClass = v.findViewById(R.id.etQuickClass);
        tvCode.setText(getString(R.string.student_code_fmt, code));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quick_student_title)
                .setMessage(R.string.quick_student_msg)
                .setView(v)
                .setCancelable(false)
                .setNegativeButton(R.string.btn_cancel, (d, w) -> resumeScanning())
                .setPositiveButton(R.string.quick_student_save, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String cls = etClass.getText() == null ? "" : etClass.getText().toString().trim().toUpperCase();
            if (name.isEmpty()) {
                tilName.setError(getString(R.string.error_required));
                return;
            }
            Student s = new Student(code, name, cls);
            db.insertStudent(s);
            dialog.dismiss();
            recordAttendance(code, name, cls);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}
