package com.can.cblog.picture.form;

import com.can.cblog.base.vo.FileVO;
import lombok.Data;

/**
 * @author ccc
 */
@Data
public class SearchPictureForm extends FileVO {

    private String searchKey;

    private Integer count;
}
