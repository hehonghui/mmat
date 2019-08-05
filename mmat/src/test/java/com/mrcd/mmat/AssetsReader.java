package com.mrcd.mmat;

import com.mrcd.mmat.util.FileUtils;


import java.io.File;

/**
 * create by mrsimple at 2019-08-03.
 */
class AssetsReader {

    /**
     * 打开test/assets 目录下的文件
     *
     * @param fileName 文件名
     * @return
     */
    static File open(String fileName) {
        return new File(FileUtils.getRuntimeWorkDir(), "src" + File.separator + "test"
                + File.separator + "assets" + File.separator + fileName) ;
    }
}
