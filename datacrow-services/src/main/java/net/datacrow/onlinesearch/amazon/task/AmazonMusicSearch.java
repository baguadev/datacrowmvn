/******************************************************************************
 *                                     __                                     *
 *                              <-----/@@\----->                              *
 *                             <-< <  \\//  > >->                             *
 *                               <-<-\ __ /->->                               *
 *                               Data /  \ Crow                               *
 *                                   ^    ^                                   *
 *                              info@datacrow.net                             *
 *                                                                            *
 *                       This file is part of Data Crow.                      *
 *       Data Crow is free software; you can redistribute it and/or           *
 *        modify it under the terms of the GNU General Public                 *
 *       License as published by the Free Software Foundation; either         *
 *              version 3 of the License, or any later version.               *
 *                                                                            *
 *        Data Crow is distributed in the hope that it will be useful,        *
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *           MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.             *
 *           See the GNU General Public License for more details.             *
 *                                                                            *
 *        You should have received a copy of the GNU General Public           *
 *  License along with this program. If not, see http://www.gnu.org/licenses  *
 *                                                                            *
 ******************************************************************************/

package net.datacrow.onlinesearch.amazon.task;

import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;

import net.datacrow.core.objects.helpers.MusicAlbum;
import net.datacrow.core.objects.helpers.MusicTrack;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AmazonMusicSearch extends AmazonSearch {

    public AmazonMusicSearch(IOnlineSearchClient listener,
                             IServer server,
                             Region region,
                             SearchMode mode,
                             String query) {
            
        super(listener, server, region, mode, query);
    }

    @Override
    protected DcObject getItem(Object o, boolean full) throws Exception {
        Document document = o instanceof Document ? (Document) o : getDocument((String) o);
        
        DcObject album = DcModules.get(getServer().getModule()).getItem();
        boolean isAudioCD = false;
        
        setValue(document, "ASIN", album, DcObject._SYS_EXTERNAL_REFERENCES);
        String asin = album.getExternalReference(DcRepository.ExternalReferences._ASIN);
        setServiceURL(album, asin);
        
        setValue(document, "DetailPageURL", album, MusicAlbum._N_WEBPAGE);
        setValue(document, "ItemAttributes/Title", album, DcMediaObject._A_TITLE);
        setYear(document, "ItemAttributes/ReleaseDate", album, DcMediaObject._C_YEAR);
        setValue(document, "ItemAttributes/EAN", album, MusicAlbum._P_EAN);
        setRating(document, album, DcMediaObject._E_RATING);
        
        Collection<String> artists = getValues(document, "ItemAttributes/Artist");
        for (String artist : artists)
            album.createReference(MusicAlbum._F_ARTISTS, artist);
        
        Collection<String> genres = getValues(document, "BrowseNodes/BrowseNode[Ancestors/BrowseNode/Ancestors/BrowseNode/Ancestors/BrowseNode/Name='Styles']/Name");
        for (String genre : genres)
            album.createReference(MusicAlbum._G_GENRES, genre);
        
        for (String genre : genres)
            album.createReference(MusicAlbum._G_GENRES, genre);


        Collection<String> discs = getValues(document, "Tracks/Disc");
        for (int disc = 1; disc - 1 < discs.size(); disc++) {
            NodeList nodes = getNodeList(document, "Tracks/Disc[@Number='" + disc + "']/Track");
            
            for (int idx = 0; idx < nodes.getLength(); idx++) {
                DcObject track = album.getModule().getChild().getItem();
                
                Node node = nodes.item(idx);
                int trackNr = Integer.valueOf(node.getAttributes().getNamedItem("Number").getTextContent());
                trackNr = discs.size() > 1 ? trackNr + (100 * disc) : trackNr; 
                
                track.setValue(MusicTrack._F_TRACKNUMBER, Long.valueOf(trackNr));
                track.setValue(MusicTrack._A_TITLE, node.getTextContent());
                
                for (String artist : artists)
                    track.createReference(MusicTrack._G_ARTIST, artist);
                
                for (String genre : genres)
                    album.createReference(MusicTrack._H_GENRES, genre);
                
                album.addChild(track);
            }
        }
        
        if (full) {
            setDescription(document, album, DcMediaObject._B_DESCRIPTION);
            setImages(album, document, MusicAlbum._J_PICTUREFRONT, null);
        }
        
        return album;
    }
}
