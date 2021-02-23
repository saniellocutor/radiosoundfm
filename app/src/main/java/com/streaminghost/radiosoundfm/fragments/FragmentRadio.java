package com.streaminghost.radiosoundfm.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.activities.MainActivity;
import com.streaminghost.radiosoundfm.services.PlaybackStatus;
import com.streaminghost.radiosoundfm.services.RadioManager;
import com.streaminghost.radiosoundfm.services.metadata.Metadata;
import com.streaminghost.radiosoundfm.services.parser.UrlParser;
import com.streaminghost.radiosoundfm.utilities.CollapseControllingFragment;
import com.streaminghost.radiosoundfm.utilities.GDPR;
import com.streaminghost.radiosoundfm.utilities.NativeTemplateStyle;
import com.streaminghost.radiosoundfm.utilities.PermissionsFragment;
import com.streaminghost.radiosoundfm.utilities.SharedPref;
import com.streaminghost.radiosoundfm.utilities.SleepTimeReceiver;
import com.streaminghost.radiosoundfm.utilities.TemplateView;
import com.streaminghost.radiosoundfm.utilities.Tools;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.labo.kaji.relativepopupwindow.RelativePopupWindow;
import com.makeramen.roundedimageview.RoundedImageView;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import es.claucookie.miniequalizerlibrary.EqualizerView;

import static android.content.Context.ALARM_SERVICE;

/**
 * This fragment is used to listen to a radio station
 */
public class FragmentRadio extends Fragment implements OnClickListener, PermissionsFragment, CollapseControllingFragment, Tools.EventListener {

    private RadioManager radioManager;
    private String radio_url = Config.RADIO_STREAM_URL;
    private Activity activity;
    private RoundedImageView albumArtView;
    private RelativeLayout relativeLayout;
    private ProgressBar progressBar;
    private FloatingActionButton buttonPlayPause;
    private Toolbar toolbar;
    private MainActivity mainActivity;
    private ImageButton img_volume_bar;
    private ImageButton img_timer;
    private InterstitialAd interstitialAd;
    private int counter = 1;
    Handler handler = new Handler();
    SharedPref sharedPref;
    EqualizerView equalizerView;
    Tools tools;

    public FragmentRadio() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        relativeLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_radio, container, false);

        toolbar = relativeLayout.findViewById(R.id.toolbar);
        setupToolbar();

        setHasOptionsMenu(true);

        sharedPref = new SharedPref(getActivity());
        sharedPref.setCheckSleepTime();
        tools = new Tools(getActivity());

        initializeUIElements();

        if (Config.ENABLE_AUTO_PLAY) {
            new Handler().postDelayed(() -> buttonPlayPause.performClick(), 1000);
        }

        //Initialize visualizer or imageview for album art
        if (Config.ENABLE_ALBUM_ART) {
            albumArtView.setVisibility(View.VISIBLE);
        } else {
            albumArtView.setVisibility(View.GONE);
        }

        albumArtView.setImageResource(Tools.BACKGROUND_IMAGE_ID);

        loadInterstitialAd();

        onBackPressed();

        Log.d("INFO_CURRENT_STREAMING", "radio name : " + sharedPref.getRadioName());
        Log.d("INFO_CURRENT_STREAMING", "radio url : " + sharedPref.getRadioURL());

        return relativeLayout;
    }

    private void setupToolbar() {
        toolbar.setTitle(getString(R.string.app_name));
        mainActivity.setSupportActionBar(toolbar);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity.setupNavigationDrawer(toolbar);
        activity = getActivity();

        Tools.isOnlineShowDialog(activity);

        //Get the radioManager
        radioManager = RadioManager.with();

        progressBar.setVisibility(View.VISIBLE);
        //Obtain the actual radio url
        AsyncTask.execute(() -> {
            radio_url = (UrlParser.getUrl(radio_url));
            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.INVISIBLE);
                updateButtons();
            });
        });

        if (isPlaying()) {
            onAudioSessionId(RadioManager.getService().getAudioSessionId());
        }

    }

    @Override
    public void onEvent(String status) {
        switch (status) {
            case PlaybackStatus.LOADING:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case PlaybackStatus.ERROR:
                makeSnackBar(R.string.error_retry);
                break;
        }

        if (!status.equals(PlaybackStatus.LOADING))
            progressBar.setVisibility(View.INVISIBLE);
        updateButtons();

    }

    @Override
    public void onAudioSessionId(Integer i) {
    }

    @Override
    public void onStart() {
        super.onStart();
        Tools.registerAsListener(this);
    }

    @Override
    public void onStop() {
        Tools.unregisterAsListener(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (!radioManager.isPlaying())
            radioManager.unbind(getContext());
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtons();
        radioManager.bind(getContext());
    }

    private void initializeUIElements() {
        progressBar = relativeLayout.findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setVisibility(View.VISIBLE);

        equalizerView = relativeLayout.findViewById(R.id.equalizer_view);

        albumArtView = relativeLayout.findViewById(R.id.albumArt);
        albumArtView.setCornerRadius((float) Config.ALBUM_ART_CORNER_RADIUS);
        albumArtView.setBorderWidth((float) Config.ALBUM_ART_BORDER_WIDTH);

        if (Config.ENABLE_CIRCULAR_IMAGE_ALBUM_ART) {
            albumArtView.setOval(true);
        } else {
            albumArtView.setOval(false);
        }

        img_volume_bar = relativeLayout.findViewById(R.id.img_volume);
        img_volume_bar.setOnClickListener(view -> changeVolume());

        img_timer = relativeLayout.findViewById(R.id.img_timer);
        img_timer.setOnClickListener(view -> {
            if (sharedPref.getIsSleepTimeOn()) {
                openTimeDialog();
            } else {
                openTimeSelectDialog();
            }
        });

        buttonPlayPause = relativeLayout.findViewById(R.id.btn_play_pause);
        buttonPlayPause.setOnClickListener(this);

        equalizerView.stopBars();
        updateButtons();
    }

    private void updateButtons() {
        if (isPlaying() || progressBar.getVisibility() == View.VISIBLE) {
            //If another stream is playing, show this in the layout
            if (RadioManager.getService() != null && radio_url != null && !radio_url.equals(RadioManager.getService().getStreamUrl())) {
                buttonPlayPause.setImageResource(R.drawable.ic_play_white);
                relativeLayout.findViewById(R.id.already_playing_tooltip).setVisibility(View.VISIBLE);
                //If this stream is playing, adjust the buttons accordingly
            } else {
                if (RadioManager.getService() != null && RadioManager.getService().getMetaData() != null) {
                    onMetaDataReceived(RadioManager.getService().getMetaData(), RadioManager.getService().getAlbumArt());
                }
                buttonPlayPause.setImageResource(R.drawable.ic_pause_white);
                relativeLayout.findViewById(R.id.already_playing_tooltip).setVisibility(View.GONE);
            }
        } else {
            //If this stream is paused, adjust the buttons accordingly
            buttonPlayPause.setImageResource(R.drawable.ic_play_white);
            relativeLayout.findViewById(R.id.already_playing_tooltip).setVisibility(View.GONE);

            updateMediaInfoFromBackground(null, null);
        }

        if (isPlaying()) {
            equalizerView.animateBars();
        } else {
            equalizerView.stopBars();
        }

    }

    @Override
    public void onClick(View v) {
        requestStoragePermission();
    }

    private void startStopPlaying() {
        radioManager.playOrPause(radio_url);
        updateButtons();
    }

    private void stopService() {
        radioManager.stopServices();
        Tools.unregisterAsListener(this);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                Intent sendInt = new Intent(Intent.ACTION_SEND);
                sendInt.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                sendInt.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + "\nhttps://play.google.com/store/apps/details?id=" + getActivity().getPackageName());
                sendInt.setType("text/plain");
                startActivity(Intent.createChooser(sendInt, "Share"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //@param info - the text to be updated. Giving a null string will hide the info.
    public void updateMediaInfoFromBackground(String info, Bitmap image) {

        TextView nowPlayingTitle = relativeLayout.findViewById(R.id.now_playing_title);
        TextView nowPlaying = relativeLayout.findViewById(R.id.now_playing);

        if (info != null)
            nowPlaying.setText(info);

        if (info != null && nowPlayingTitle.getVisibility() == View.GONE) {
            nowPlayingTitle.setVisibility(View.VISIBLE);
            nowPlaying.setVisibility(View.VISIBLE);
        } else if (info == null) {
            nowPlayingTitle.setVisibility(View.VISIBLE);
            nowPlayingTitle.setText(R.string.now_playing);
            nowPlaying.setVisibility(View.VISIBLE);
            nowPlaying.setText(R.string.app_name);
        }

        if (image != null) {
            albumArtView.setImageBitmap(image);
        } else {
            albumArtView.setImageResource(Tools.BACKGROUND_IMAGE_ID);
        }

    }

    @Override
    public String[] requiredPermissions() {
        return new String[]{Manifest.permission.READ_PHONE_STATE};
    }

    @Override
    public void onMetaDataReceived(Metadata meta, Bitmap image) {
        //Update the mediainfo shown above the controls
        String artistAndSong = null;
        if (meta != null && meta.getArtist() != null)
            artistAndSong = meta.getArtist() + " - " + meta.getSong();
        updateMediaInfoFromBackground(artistAndSong, image);
    }

    private boolean isPlaying() {
        return (null != radioManager && null != RadioManager.getService() && RadioManager.getService().isPlaying());
    }

    @Override
    public boolean supportsCollapse() {
        return false;
    }

    private void makeSnackBar(int text) {
        Snackbar bar = Snackbar.make(buttonPlayPause, text, Snackbar.LENGTH_SHORT);
        bar.show();
        ((TextView) bar.getView().findViewById(R.id.snackbar_text)).setTextColor(getResources().getColor(R.color.white));
    }

    public void onBackPressed() {
        ((MainActivity) getActivity()).setOnBackClickListener(() -> {
            if (Config.ENABLE_ADMOB_NATIVE_AD_ON_CLOSE_DIALOG) {
                exitDialogWithNativeAd();
            } else {
                exitDialog();
            }
            return true;
        });
    }

    public void exitDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setIcon(R.mipmap.ic_launcher);
        dialog.setTitle(R.string.app_name);
        dialog.setMessage(getResources().getString(R.string.message));
        dialog.setPositiveButton(getResources().getString(R.string.quit), (dialogInterface, i) -> {
            stopService();
            getActivity().finish();
        });
        dialog.setNegativeButton(getResources().getString(R.string.minimize), (dialogInterface, i) -> minimizeApp());
        dialog.setNeutralButton(getResources().getString(R.string.cancel), (dialogInterface, i) -> {
        });
        dialog.show();
    }

    public void exitDialogWithNativeAd() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.custom_exit_dialog, null);
        TemplateView native_template = view.findViewById(R.id.my_template);
        MediaView mediaView = view.findViewById(R.id.media_view);
        AdLoader adLoader = new AdLoader.Builder(getActivity(), getActivity().getResources().getString(R.string.admob_native_ad_unit_id))
                .forUnifiedNativeAd(unifiedNativeAd -> {
                        ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.white));
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();
                        native_template.setStyles(styles);
                    mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
                    native_template.setNativeAd(unifiedNativeAd);
                    native_template.setVisibility(View.VISIBLE);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        native_template.setVisibility(View.GONE);
                    }
                })
                .build();
        adLoader.loadAd(GDPR.getAdRequest(getActivity()));

        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setView(view);
        dialog.setCancelable(false);
        dialog.setPositiveButton(getResources().getString(R.string.quit), (dialogInterface, i) -> {
            stopService();
            getActivity().finish();
        });
        dialog.setNegativeButton(getResources().getString(R.string.minimize), (dialogInterface, i) -> minimizeApp());
        dialog.setNeutralButton(getResources().getString(R.string.cancel), (dialogInterface, i) -> {
        });
        dialog.show();
    }

    public void minimizeApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void requestStoragePermission() {
        Dexter.withActivity(getActivity())
                .withPermissions(Manifest.permission.READ_PHONE_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted

                        if (report.areAllPermissionsGranted()) {
                            if (!isPlaying()) {
                                if (radio_url != null) {
                                    startStopPlaying();
                                    showInterstitialAd();
                                    //Check the sound level
                                    AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                                    int volume_level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    if (volume_level < 2) {
                                        makeSnackBar(R.string.volume_low);
                                    }
                                } else {
                                    //The loading of urlToPlay should happen almost instantly, so this code should never be reached
                                    makeSnackBar(R.string.error_retry_later);
                                }
                            } else {
                                startStopPlaying();
                            }
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(error -> Toast.makeText(getActivity(), "Error occurred! " + error.toString(), Toast.LENGTH_SHORT).show())
                .onSameThread()
                .check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private void changeVolume() {
        final RelativePopupWindow popupWindow = new RelativePopupWindow(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.lyt_volume, null);
        ImageView imageView1 = view.findViewById(R.id.img_volume_max);
        ImageView imageView2 = view.findViewById(R.id.img_volume_min);
        imageView1.setColorFilter(Color.BLACK);
        imageView2.setColorFilter(Color.BLACK);

        VerticalSeekBar seekBar = view.findViewById(R.id.seek_bar_volume);
        seekBar.getThumb().setColorFilter(sharedPref.getFirstColor(), PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(sharedPref.getSecondColor(), PorterDuff.Mode.SRC_IN);

        final AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        seekBar.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar.setProgress(volume_level);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        popupWindow.setFocusable(true);
        popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setContentView(view);
        popupWindow.showOnAnchor(img_volume_bar, RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
    }

    public void openTimeSelectDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(getActivity());
        alt_bld.setTitle(getString(R.string.sleep_time));

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_dialog_select_time, null);
        alt_bld.setView(dialogView);

        final TextView tv_min = dialogView.findViewById(R.id.txt_minutes);
        tv_min.setText("1 " + getString(R.string.min));
        FrameLayout frameLayout = dialogView.findViewById(R.id.frameLayout);

        final IndicatorSeekBar seekbar = IndicatorSeekBar
                .with(getActivity())
                .min(1)
                .max(120)
                .progress(1)
                .thumbColor(sharedPref.getSecondColor())
                .indicatorColor(sharedPref.getFirstColor())
                .trackProgressColor(sharedPref.getFirstColor())
                .build();

        seekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                tv_min.setText(seekParams.progress + " " + getString(R.string.min));
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {

            }
        });

        frameLayout.addView(seekbar);

        alt_bld.setPositiveButton(getString(R.string.set), (dialog, which) -> {
            String hours = String.valueOf(seekbar.getProgress() / 60);
            String minute = String.valueOf(seekbar.getProgress() % 60);

            if (hours.length() == 1) {
                hours = "0" + hours;
            }

            if (minute.length() == 1) {
                minute = "0" + minute;
            }

            String totalTime = hours + ":" + minute;
            long total_timer = tools.convertToMilliSeconds(totalTime) + System.currentTimeMillis();

            Random random = new Random();
            int id = random.nextInt(100);

            sharedPref.setSleepTime(true, total_timer, id);

            Intent intent = new Intent(getActivity(), SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), id, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, total_timer, pendingIntent);
            }
        });
        alt_bld.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {

        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    public void openTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.sleep_time));
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.lyt_dialog_time, null);
        builder.setView(dialogView);

        TextView textView = dialogView.findViewById(R.id.txt_time);

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {

        });

        builder.setPositiveButton(getString(R.string.stop), (dialog, which) -> {
            Intent i = new Intent(getActivity(), SleepTimeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), sharedPref.getSleepID(), i, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
            pendingIntent.cancel();
            alarmManager.cancel(pendingIntent);
            sharedPref.setSleepTime(false, 0, 0);
        });

        updateTimer(textView, sharedPref.getSleepTime());

        builder.show();
    }

    private void updateTimer(final TextView textView, long time) {
        long timeleft = time - System.currentTimeMillis();
        if (timeleft > 0) {
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeleft),
                    TimeUnit.MILLISECONDS.toMinutes(timeleft) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeleft) % TimeUnit.MINUTES.toSeconds(1));

            textView.setText(hms);
            handler.postDelayed(() -> {
                if (sharedPref.getIsSleepTimeOn()) {
                    updateTimer(textView, sharedPref.getSleepTime());
                }
            }, 1000);
        }
    }

    private void loadInterstitialAd() {
        if (Config.ENABLE_ADMOB_INTERSTITIAL_ON_PLAY) {
            interstitialAd = new InterstitialAd(getActivity());
            interstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_unit_id));
            interstitialAd.loadAd(GDPR.getAdRequest(getActivity()));
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    interstitialAd.loadAd(GDPR.getAdRequest(getActivity()));
                }
            });
        } else {
            Log.d("INFO", "AdMob Interstitial is Disabled");
        }
    }

    private void showInterstitialAd() {
        if (Config.ENABLE_ADMOB_INTERSTITIAL_ON_PLAY) {
            if (interstitialAd != null && interstitialAd.isLoaded()) {
                if (counter == Config.ADMOB_INTERSTITIAL_ON_PLAY_INTERVAL) {
                    interstitialAd.show();
                    counter = 1;
                } else {
                    counter++;
                }
            } else {
                Log.d("INFO", "Interstitial Ad is Disabled");
            }
        } else {
            Log.d("INFO", "AdMob Interstitial is Disabled");
        }
    }

}