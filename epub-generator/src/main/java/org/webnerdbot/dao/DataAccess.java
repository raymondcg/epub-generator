package org.webnerdbot.dao;

import java.nio.charset.Charset;
import java.util.List;

public interface DataAccess {

    public boolean fileExist( String folderLocation, String fileName );

    public List<String> getFoldersInFolder( String folderLocation );

    public List<String> getFilesInFolder( String folderLocation );

    public void saveFile( String folderLocation, String fileName, String contents );

    public void saveFile( String folderLocation, String fileName, String contents, Charset charset );

    public String readFile( String folderLocation, String fileName );

    public String readFile( String folderLocation, String fileName, Charset charset );

}
