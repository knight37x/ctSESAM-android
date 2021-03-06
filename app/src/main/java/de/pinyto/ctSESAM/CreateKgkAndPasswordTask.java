package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Create kgk and password at once.
 */
class CreateKgkAndPasswordTask extends AsyncTask<byte[], Void, byte[]> {
    int iterations;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<MainActivity> mainActivityWeakRef;

    CreateKgkAndPasswordTask(MainActivity mainActivity,
                             int iterations,
                             KgkManager kgkManager,
                             PasswordSettingsManager settingsManager) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
        this.iterations = iterations;
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
    }

    @Override
    protected byte[] doInBackground(byte[]... params) {
        byte[] password = params[0];
        byte[] salt = Crypter.createSalt();
        byte[] ivKey = Crypter.createIvKey(password, salt);
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
        return ivKey;
    }

    @Override
    protected void onPostExecute(byte[] ivKey) {
        kgkManager.createAndStoreNewKgkBlock(new Crypter(ivKey));
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) activity.findViewById(R.id.autoCompleteTextViewDomain);
            String domainStr = autoCompleteTextViewDomain.getText().toString();
            byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
            PasswordSetting setting = settingsManager.getSetting(domainStr);
            EditText editTextUsername =
                    (EditText) activity.findViewById(R.id.editTextUsername);
            byte[] username = UTF8.encode(editTextUsername.getText());
            GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(activity);
            generatePasswordTask.execute(
                    domain,
                    username,
                    kgkManager.getKgk(),
                    setting.getSalt(),
                    ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(iterations).array());
        }
    }
}
