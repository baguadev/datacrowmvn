package net.datacrow.onlinesearch.google.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;
import net.datacrow.onlinesearch.google.task.GoogleSearch;

public class GoogleServer
        implements IServer
{
    private static final long serialVersionUID = 6451130355747891181L;
    private Collection<Region> regions = new ArrayList();
    private Collection<SearchMode> modes = new ArrayList();

    public GoogleServer()
    {
        this.regions.add(new Region("en", "English", "http://www.google.com/"));
    }

    public int getModule()
    {
        return DcModules._BOOK;
    }

    public Collection<Setting> getSettings()
    {
        return null;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public boolean isFullModeOnly()
    {
        return false;
    }

    public String getName()
    {
        return "Google Books";
    }

    public Collection<Region> getRegions()
    {
        return this.regions;
    }

    public Collection<SearchMode> getSearchModes()
    {
        return this.modes;
    }

    public String getUrl()
    {
        return "http://www.google.com";
    }

    public SearchTask getSearchTask(IOnlineSearchClient listener, SearchMode mode, Region region, String query, DcObject client)
    {
        GoogleSearch task = new GoogleSearch(listener, this, mode, query);
        task.setClient(client);
        return task;
    }

    public String toString()
    {
        return getName();
    }
}
