/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jbull.audioplayer;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

/**
 *
 * @author jordan
 */
public class Launch extends AnchorPane implements Component {
    
    @FXML ProgressBar progressBar;
    @FXML Label loadInfo;
    
    private int totalItems = 0;
    private int progress = 0;
    private final int PLUGIN_VALUE = 5;
    private final int SONG_VALUE = 1;
    private final int SETTING_VALUE = 2;
    Application application;
    
    Connection connection;
    
    protected Launch(Application application) {
        this.application = application;
        FXMLLoader fxmlLoader = new FXMLLoader(getFXML());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
    
    @Override
    public URL getFXML() {
        return this.getClass().getResource("Launch.fxml");
    }
    
    protected void launch() {
        try {
            Database.connectToDatabase();
            Database.Library.createTracksTable();
            Database.Playlists.createPlaylistsTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        totalItems = (PLUGIN_VALUE *getNumPlugins()) +
                (SONG_VALUE * getNumSongs()) + (SETTING_VALUE * getNumSettings());
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(.5), new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event) {
                progressBar.setProgress(progress/totalItems);
            }
        }));
        loadInfo.setText("Launching Plugins...");
        launchPlugins();
        loadInfo.setText("Loading Songs...");
        fillLibrary();
        loadInfo.setText("Loading Default Settings");
        defaultSettings();
        AudioPlayer.stg.setScene(new Scene(application));
        AudioPlayer.stg.show();
    }
    
    private void initializationTasks() {
        
    }
    
    private void launchPlugins() {
        
    }
    
    private void fillLibrary() {
        
    }
    
    private void defaultSettings() {
        
    }
    
    private int getNumPlugins() {
        //TODO
        return 0;
    }
    
    private int getNumSongs() {
        //TODO
        return 0;
    }
    
    private int getNumSettings() {
        //TODO
        return 0;
    }
    
}
