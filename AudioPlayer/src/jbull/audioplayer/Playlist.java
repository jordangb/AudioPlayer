package jbull.audioplayer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author jordan
 */
public abstract class Playlist extends AnchorPane implements Component {
    
    private ArrayList<TrackView> tracks;
    private int position = -1;
    private String playlistName;
    private boolean resetOnPlaylistChange = true; //This should be true almost all of the time except in particular circumstances
    private static HashMap<String, Integer> playlistMapping = new HashMap<String, Integer>();
    
    public Playlist() {
        FXMLLoader fxmlLoader = new FXMLLoader(getFXML());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        tracks = new ArrayList<TrackView>();
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        final Playlist me = this;
        
        this.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (Application.draggedObject instanceof TrackView) {
                    Dragboard db = event.getDragboard();
                    TrackView draggedTrack = (TrackView) Application.draggedObject;
                    if (draggedTrack.isInPlaylist() && draggedTrack.getPlaylistInfo().getName().equals(me.getName())) { //moved from this playlist
                        System.out.println("drag over from playlist");
                        event.acceptTransferModes(TransferMode.ANY);
                    } else if (!db.hasString()) { // moved from library
                        event.acceptTransferModes(TransferMode.COPY);
                    }
                    event.consume();
                }
            }
        });
        
        this.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (Application.draggedObject instanceof TrackView) {
                    TrackView track = (TrackView) Application.draggedObject;
                    Dragboard db = event.getDragboard();
                    if (track.isInPlaylist() /*&& /*track.getPlaylistInfo().getName().equals(getName())*/) { //moved from this playlist
                        event.acceptTransferModes(TransferMode.MOVE);
                        TrackView trackView = Application.createTrackView((TrackView) Application.draggedObject);
                        me.addTrack(trackView);
                    } else if (!db.hasString()) { // moved from library
                        TrackView trackView = Application.createTrackView((TrackView) Application.draggedObject);
                        me.addTrack(trackView);
                    }
                    event.consume();
                }
                event.setDropCompleted(true);
             }
        });
    }
    
    protected TrackView current() {
        if (position >= 0 && position < tracks.size()) {
            return tracks.get(position);
        } else {
            return first();
        }
    }
    
    /**
     * Sets the position to -1 and calls next().
     * @return  the first track in the playlist if there is one or null if there
     * are no tracks in the playlist
     */
    protected TrackView first() {
        position = -1;
        if (tracks.isEmpty()) {
            return null;
        } else {
            return next();
        }
    }
    
    /**
     * Increments the position and returns the the next track. If there are no
     * more tracks, the position is set to -1 and null is returned.
     * @return  the next track if there is one and null if there is no next track
     */
    protected TrackView next() {
        if (hasNext()) {
            position ++;
            return tracks.get(position);
        } else {
            position = -1;
            return null;
        }
    }
    
    /**
     * Returns true if there is a next track or false if there is not.
     * @return true if there is a next track or false if there is not
     */
    protected boolean hasNext() {
        return (position + 1) < tracks.size();
    }
    
    /**
     * Decrements the position and returns the previous track if there is one.
     * If there is not a previous track, the position is set to -1 and null is
     * returned.
     * @return  the previous track if there is one or null otherwise
     */
    protected TrackView prev() {
        if (hasPrev()) {
            position --;
            return tracks.get(position);
        } else {
            position = -1;
            return null;
        }
    }
    
    /**
     * Returns true if there is a previous track or false is there is not.
     * @return  true if there is a previous track or false is there is not
     */
    protected boolean hasPrev() {
        return position > 0 && position <= tracks.size();
    }
    
    /**
     * Returns the number of tracks the library contains. Note: this is not
     * necessarily the number of {@link TrackView}s in the library GUI.
     * @return  the number of tracks in the library
     */
    protected int numTracks() {
        return tracks.size();
    }
    
    
    /**
     * Adds track to the specified index pushing down all tracks at or below the
     * index. This should also set the playlist field of the TrackView. The
     * track's playlist field is also set to this playlist.
     * @param track the {@link TrackView} to be placed in the playlist
     * @param index the index the track is to be placed at
     */
    public void addTrack(TrackView track, int index) {
        if (index <= position) {
            position++;
        }
        for (int i = index; i < tracks.size(); i++) {
            tracks.get(i).getPlaylistInfo().setPosition(i+1);
        }
        tracks.add(index, track);
        track.setPlaylist(this, index);
        addTrackToGUI(track, index);
        try {
            Database.Playlists.addSongToPlaylist(this.getCurrentPlaylistID(), track.getSongID(), index);
        } catch (SQLException ex) {
            //TODO inform user of error
            Logger.getLogger(Playlist.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Appends a track to the end of the playlist. The
     * track's playlist field is also set to this playlist.
     * @param track the track to append to the playlist
     */
    public void addTrack(TrackView track) {
        addTrack(track, this.numTracks());
    }
    
    protected void restoreTrackFromDatabase(TrackView track) {
        track.setPlaylist(this, this.numTracks());
        addTrackToGUI(track);
        tracks.add(track);
    }
    
    protected void restoreAllPlaylistTracksFromDatabase() {
        tracks.clear();
        this.emptyTracksFromGUI();
        try {
            ArrayList<Integer> trackIDs =
                Database.Playlists.getPlaylistSongs(this.getCurrentPlaylistID());
            for (Integer trackID : trackIDs) {
                this.restoreTrackFromDatabase(Application.createTrackView(Database.Library.getTrack(trackID)));
            }
        } catch (SQLException e) {
            //TODO inform user that there was an error loading the playlist songs
            e.printStackTrace();
        }
    }
    
    /**
     * Adds track to the specified index pushing down all tracks at or below the
     * index. This should also set the playlist field of the TrackView.
     * @param track the {@link TrackView} to be placed in the playlist
     * @param index the index the track is to be placed at
     */
    protected abstract void addTrackToGUI(TrackView track, int index);
    
    /**
     * Appends a track to the end of the playlist.
     * @param track the track to append to the playlist
     */
    protected abstract void addTrackToGUI(TrackView track);
    
    /**
     * Removes the track at the specified index pushing all tracks below the
     * index up one.
     * @param index the index of the track to be removed 
     */
    public void removeTrack(int index) {
        try {
            Database.Playlists.removeSongFromPlaylist(this.getCurrentPlaylistID(), index);
        } catch (SQLException e) {
            //TODO inform user that the change will not be reflected in the database.
            //TODO reflect changes to all playlist guis
            e.printStackTrace();
        }
        if (index <= position) {
            position--;
        }
        tracks.remove(index);
        for (int i = index; i < tracks.size(); i++) {
            int was = tracks.get(i).getPlaylistInfo().getPosition();
            tracks.get(i).getPlaylistInfo().setPosition(i);
        }
        removeTrackFromGUI(index);
    }
    
    public void removeTrack(TrackView track) {
        removeTrack(tracks.indexOf(track));
    }
    
    protected abstract void removeTrackFromGUI(int index);
    
    protected void clearPlaylist() {
        tracks.clear();
        emptyTracksFromGUI();
    }
    
    protected abstract void emptyTracksFromGUI();
    
    /**
     * Returns the name of the currently displayed playlist.
     * @return  the name of the currently displayed playlist
     */
    public String getName() {
        return playlistName;
    }
    
    
    protected void addPlaylist(String playlistName, int playlistID) {
        playlistMapping.put(playlistName, playlistID);
        addPlaylistToGUI(playlistName);
    }
    
    protected abstract void addPlaylistToGUI(String playlistName);
    
    protected static void addPlaylistToAllPanes(String playlistName, int playlistID) {
        ArrayList<Playlist> playlistPanes = Application.contentPane.getPlaylistPanes();
        for (Playlist playlist : playlistPanes) {
            playlist.addPlaylist(playlistName, playlistID);
        }
    }
    
    protected void removePlaylist(String playlistName) {
        playlistMapping.remove(playlistName);
        removePlaylistFromGUI(playlistName);
    }
    
    protected abstract void removePlaylistFromGUI(String playlistName);
    
    public void setPlaylist(String playlistName) {
        if (!playlistMapping.containsKey(playlistName)) {
            throw new RuntimeException();
        }
        this.playlistName = playlistName;
        System.out.println(this.playlistName);
        setSelectedPlaylistGUI(playlistName);
        clearPlaylist();
        if (resetOnPlaylistChange) { //fills playlist and resets playlist position
            System.out.println("in");
            this.restoreAllPlaylistTracksFromDatabase();
            this.position = -1;
        }
    }
    
    /**
     * Sets the GUI component that displays which playlist is currently selected.
     * @param playlistName 
     */
    protected abstract void setSelectedPlaylistGUI(String playlistName);
    
    protected abstract void renamePlaylistInGUI(String oldName, String newName);
    
    protected void removeAllPlaylists() {
        for (String playlist : playlistMapping.keySet()) {
            removePlaylist(playlist);
        }
    }
    
    protected int getCurrentPlaylistID() {
        System.out.println(playlistMapping.get(this.getName()));
        return playlistMapping.get(this.getName());
    }
    
    private void renamePlaylist(String oldName, String newName) {
        resetOnPlaylistChange = false;
        try {
        renamePlaylistInGUI(oldName, newName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resetOnPlaylistChange = true;
        }
    }
    
    protected static void renamePlaylistInAllPanes(String oldName, String newName) {
        ArrayList<Playlist> playlistPanes = Application.contentPane.getPlaylistPanes();
        playlistMapping.put(newName, playlistMapping.remove(oldName));
        for (Playlist playlist : playlistPanes) {
            playlist.renamePlaylist(oldName, newName);
        }
        
        try {
            Database.Playlists.renamePlaylist(oldName, newName);
        } catch (SQLException e) {
            e.printStackTrace();
            //TODO handle this in gui
        }
    }
}
