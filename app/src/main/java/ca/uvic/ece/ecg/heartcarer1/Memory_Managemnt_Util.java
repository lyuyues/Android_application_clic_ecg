package ca.uvic.ece.ecg.heartcarer1;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Memory_Managemnt_Util  {

	public Memory_Managemnt_Util () {
		
	}

	public long getFileSize(File f) throws Exception { 
		long size = 0; 
		File flist[] = f.listFiles(); 
		for (int i = 0; i < flist.length; i++) { 
			if (flist[i].isDirectory()) { 
				size = size + getFileSize(flist[i]); 
			} else { 
				size = size + flist[i].length(); 
			} 
		} 
		return size; 
	} 
	
	public static List<File> getFileSort(String path) {
		 
        List<File> list = getFiles(path, new ArrayList<File>());
 
        if (list != null && list.size() > 0) {
 
            Collections.sort(list, new Comparator<File>() {
                public int compare(File file, File newFile) {
                    if (file.lastModified() < newFile.lastModified()) {
                        return 1;
                    } else if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    } else {
                        return -1;
                    }
 
                }
            });
 
        }
 
        return list;
    }
	
	public static List<File> getFiles(String realpath, List<File> files) {
		 
        File realFile = new File(realpath);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles();
            for (File file : subfiles) {
                if (file.isDirectory()) {
                    getFiles(file.getAbsolutePath(), files);
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }
	
	protected void Memory_management() {
    	if (!Global.ifRegUser) {
    		return;
    	}
    	File file = new File(Global.folder);
    	try {
			if (getFileSize(file) / 1048576 < Global.max_memory) {
				return;
			}
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".bin");
				}
			};
			File[] files = new File(Global.downloadPath).listFiles(filter);
			for(File F : files){
				F.delete();
			}
			long size = getFileSize(file);
			if (getFileSize(file) / 1048576 < Global.max_memory) {
				return;
			}
			List<File> file_list =  getFileSort(Global.quickcheckpath);
			int length = file_list.size();
			long difference = size - Global.max_memory;
			for(int i = length - 1; i >= 0; i--) {
				File F = file_list.get(i);
				difference -= getFileSize(F);
				F.delete();
				if (difference < 0) {
					break;
				}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

