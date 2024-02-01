package com.appcomponents.demo.ui;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;


import com.appcomponents.demo.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import java.util.Objects;

public class ExoplayerActivity extends Activity implements View.OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener {

    // Declaration of member variables

    // The view that displays the video player
    private PlayerView playerView;
    // The ExoPlayer instance for playing media
    private SimpleExoPlayer player;
    // The root view for displaying debugging information
    private LinearLayout debugRootView;

    // The track selector that determines available audio and video tracks
    private DefaultTrackSelector trackSelector;

    // The track selector parameters to customize track selection behavior
    private DefaultTrackSelector.Parameters trackSelectorParameters;

    // Keys used for saving/restoring state information in the bundle
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    // Variables to store the initial autoplay, window, and position values
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;

    // A boolean flag to indicate if the starting position has been set
    boolean hvStartPosition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);

        // Set the activity to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Find the root view and set click listener
        View rootView = findViewById(R.id.root_exoplayer);
        rootView.setOnClickListener(this);

        // Find and initialize views related to ExoPlayer
        debugRootView = findViewById(R.id.controls_root);
        playerView = findViewById(R.id.player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.requestFocus();

        // Build default track selector parameters
        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
    }

    @Override
    public void onClick(View view) {
        // Check if the view's parent is the debugRootView
        if ( view.getParent() == debugRootView ) {
            // Get the current mapped track information from the track selector
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                // Get the text (title) of the button view
                CharSequence title = ((Button) view).getText();
                // Get the renderer index associated with the view
                int rendererIndex = (int) view.getTag();
                // Get the renderer type for the given renderer index
                int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
                // Check if adaptive selections are allowed
                boolean allowAdaptiveSelections =
                        rendererType == C.TRACK_TYPE_VIDEO
                                || (rendererType == C.TRACK_TYPE_AUDIO
                                && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS);

                // Create a new TrackSelectionDialogBuilder instance
                TrackSelectionDialogBuilder build = new TrackSelectionDialogBuilder(this, title, trackSelector, rendererIndex);

                // Set whether adaptive selections are allowed for the track selection dialog
                build.setAllowAdaptiveSelections(allowAdaptiveSelections);
                // Set whether to show the disable option in the track selection dialog
                build.setShowDisableOption(true);
                // Build the track selection dialog and show it
                build.build().show();
            }
        }
    }

    @Override
    public void preparePlayback() {
        // Call the initializePlayer method to set up the player for playback
        initializePlayer();
    }

    @Override
    public void onVisibilityChange(int visibility) {
        // Set the visibility of the debugRootView based on the provided visibility parameter
        debugRootView.setVisibility(visibility);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // Call the superclass implementation of onWindowFocusChanged to perform any necessary default behavior
        super.onWindowFocusChanged(hasFocus);
    }


    @Override
    protected void onStop() {
        super.onStop();
        // Check if the current SDK version is greater than 23 (Android Marshmallow or newer)
        if (Util.SDK_INT > 23) {
            // Check if the playerView object is not null
            if (playerView != null) {
                // Pause the playerView to stop rendering video playback
                playerView.onPause();
            }
            // Release the player, which includes stopping playback and freeing resources
            releasePlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                // Resume the playerView to start or resume rendering video playback
                playerView.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Check if the current SDK version is greater than 23 (Android Marshmallow or newer)
        if (Util.SDK_INT <= 23) {
            // Check if the playerView object is not null
            if (playerView != null) {
                // Pause the playerView to stop rendering video playback
                playerView.onPause();
            }
            // Release the player, which includes stopping playback and freeing resources
            releasePlayer();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Update the track selector parameters with any new values
        updateTrackSelectorParameters();
        // Update the starting position of the playback
        updateStartPosition();
        // Save the track selector parameters to the bundle
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
        // Save the autoplay flag to the bundle
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        // Save the window index to the bundle
        outState.putInt(KEY_WINDOW, startWindow);
        // Save the playback position to the bundle
        outState.putLong(KEY_POSITION, startPosition);
    }

    private void initializePlayer() {
        // Prepare the media source for playback
        MediaSource source = prepareSource();
        // Start playing the media source from the desired start position
        play(source, hvStartPosition);
    }

    private void play(MediaSource source, boolean isStartPosition) {
        // Prepare the player with the provided media source
        player.prepare(source, !isStartPosition, false);
    }

    private MediaSource prepareSource() {

        MediaSource mediaSource;

        if ( player == null ) {

            // Create a track selector using an adaptive track selection factory.
            TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();

            // Set the parameters of the track selector to its current parameters.
            // Calling buildUponParameters() ensures that a copy of the current parameters is used.
            // This is done to avoid modifying the original parameters directly, which might cause unexpected behavior.
            trackSelector = new DefaultTrackSelector(trackSelectionFactory);
            trackSelector.setParameters(trackSelector
                    .buildUponParameters());

            // Create a new instance of ExoPlayerFactory, passing in the application context and the trackSelector to use.
            // The trackSelector determines the tracks to be played based on the given track selection factory.
            player = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);

            // Add a player event listener to the player. This listener can be used to handle various playback events,
            // such as buffering, playback state changes, error handling, etc.
            player.addListener(new PlayerEventListener());

            // Set the player to play when ready. This means that playback starts as soon as the player is ready to play.
            // The parameter 'true' indicates that the player should start playing when it's ready.
            player.setPlayWhenReady(true);
            // Add an analytics listener to the player. This listener can be used to log events and gather analytics data
            // during playback, such as track changes, bitrate, errors, etc.
            player.addAnalyticsListener(new EventLogger(trackSelector));

            // Set the player for the player view. The player view is the UI component responsible for rendering the player
            // and handling user interactions, such as play/pause, seek, etc.
            playerView.setPlayer(player);

            // Set the playback preparer for the player view. The playback preparer is responsible for preparing the player
            // before playback, such as setting the media source, preparing video rendering, etc.
            // In this case, the 'this' refers to the current class (or interface) which implements the PlaybackPreparer interface.
            playerView.setPlaybackPreparer(this);

            // Create a data source factory using the DefaultDataSourceFactory. The first argument is the application context,
            // and the second argument is the user agent string. The user agent string is used for network requests and can be
            // a descriptive name or identifier for the application.
            DataSource.Factory dataSourceFactory =
                    new DefaultDataSourceFactory(this, "test");

            // Create an extractors factory using the DefaultExtractorsFactory. The extractors factory is responsible for creating
            // the necessary extractors to extract media data from the provided media source, such as audio and video samples.
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

            // Create a video Uri object by parsing the given video URL string. This specifies the location of the video to be played.
            Uri videoUri = Uri.parse("http://23.237.117.10/test-am-1080.mkv");

            // Create a new ExtractorMediaSource using the video Uri, data source factory, extractors factory, and optional
            // additional objects for custom handling of the media source. The ExtractorMediaSource represents a media source
            // that extracts data from a given URI using the provided factories for data source and extractors.
            mediaSource = new ExtractorMediaSource(videoUri,
                    dataSourceFactory, extractorsFactory, null, null);

            return mediaSource;
        }
        return null;
    }


    private void updateButtonVisibilities() {

        // Remove all views from the debugRootView. This is typically done to clear any previously added views for debugging purposes.
        debugRootView.removeAllViews();

        // Check if the player object is null. If it is null, there is nothing to do, so return from the method.
        if (player == null) {
            return;
        }

        // Get the current MappedTrackInfo from the trackSelector. This object provides information about the available tracks for the media being played or streamed.
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        // Check if the mappedTrackInfo object is null. If it is null, it means that there is no track information available. In this case, the method returns and exits, as there is nothing else to be done. This is a way to handle cases where there is no track information available, indicating that there may be an issue with the media being played or streamed.
        if (mappedTrackInfo == null) {
            return;
        }

        // Loop through each renderer (e.g., video, audio, subtitles) in the mappedTrackInfo.
        for ( int i = 0; i < mappedTrackInfo.getRendererCount(); i++ ) {
            // Get the track groups for the current renderer. A track group represents a set of tracks that belong to the same category, such as video tracks or audio tracks. For example, a video renderer may have multiple video track groups representing different resolutions or formats.
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);

            // Check if there are any track groups available.
            if (trackGroups.length != 0) {
                // Create a new Button object. This button will be used to display the available track options to the user.
                Button button = new Button(this);
                int label;
                // Check the renderer type of the player at index i.
                switch (player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        // If the renderer type is audio, set the label variable to the resource ID of the audio track selection title string.
                        label = com.google.android.exoplayer2.R.string.exo_track_selection_title_audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        // If the renderer type is video, set the label variable to the resource ID of the video track selection title string.
                        label = com.google.android.exoplayer2.R.string.exo_track_selection_title_video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        // If the renderer type is text, set the label variable to the resource ID of the text track selection title string.
                        label = com.google.android.exoplayer2.ui.R.string.exo_track_selection_title_text;
                        break;
                    default:
                        // If the renderer type is not one of the above, skip to the next iteration of the loop.
                        continue;
                }
                // Set the text of the button to the value of the `label` variable, which represents the track selection title string.
                button.setText(label);
                // Set the text color of the button to white.
                button.setTextColor(Color.WHITE);
                // Set the tag of the button to the index `i`.
                button.setTag(i);
                // Set the background color of the button to transparent.
                button.setBackgroundColor(getResources().getColor(R.color.transparent));
                // Set an OnClickListener on the button to handle button click events.
                button.setOnClickListener(this);
                button.setOnFocusChangeListener((v, hasFocus) -> {
                    // Set an OnFocusChangeListener on the button to handle focus change events.
                    if(hasFocus) {
                        // If the button gains focus, change the background color to the default card background color.
                        button.setBackgroundColor(getResources().getColor(R.color.default_card_background_color));
                    }
                    else {
                        // If the button loses focus, change the background color back to transparent.
                        button.setBackgroundColor(getResources().getColor(R.color.transparent));
                    }
                });
                // Add the button to the debugRootView, which is a container for the debug views.
                debugRootView.addView(button);
            }
        }
    }

    // This method updates the track selector parameters.
    private void updateTrackSelectorParameters() {
        // If the track selector is not null:
        if (trackSelector != null) {
            // Get the current parameters of the track selector.
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    // This method releases the media player.
    private void releasePlayer() {
        // If the player is not null:
        if ( player != null ) {
            // Release the player resources.
            player.release();
            // Set the player object to null.
            player = null;
        }
    }

    // This method updates the start position of the media player.
    private void updateStartPosition() {
        // If the player is not null,
        if (player != null) {
            // Get the value of the playWhenReady flag,
            // which determines whether the video should start playing automatically.
            startAutoPlay = player.getPlayWhenReady();
            // Get the index of the current window.
            startWindow = player.getCurrentWindowIndex();
            // Get the current position of the media content,
            // ensuring that it is not negative by using the Math.max() method.
            startPosition = Math.max(0, player.getContentPosition());
        }
    }



    private class PlayerEventListener implements Player.EventListener {


        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            // This method is called when the player's state changes,
            // such as when it starts playing or pauses.
            // It updates the visibilities of the buttons accordingly.
            updateButtonVisibilities();
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            // This method is called when there is a discontinuity in the playback position.
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            // This method is called when there is an error in the player.
            // If the error message contains "Top bit not zero",
            if( Objects.requireNonNull(e.getMessage()).contains("Top bit not zero:") )
            {
                // Remove all views from the overlay frame layout.
                Objects.requireNonNull(playerView.getOverlayFrameLayout()).removeAllViews();
                // Release the player and set it to null.
                if( player != null ) {
                    player.release();
                    player = null;
                }
            }
            else {
                // If the error message does not contain "Top bit not zero",
                // Update the button visibilities.
                updateButtonVisibilities();
            }
        }
    }
}