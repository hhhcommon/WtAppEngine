/**Plug**/
/**== 一、版本管理 */
/**P001 版本记录[P_VERCONFIG]*/
DROP TABLE IF EXISTS p_VerConfig;
CREATE TABLE p_VerConfig (
  pubFileName        varchar(200)  NOT NULL  COMMENT '最终发布版本Apk名称',
  pubUrl             varchar(200)  NOT NULL  COMMENT '最终发布版本的Url',
  pubStorePath       varchar(200)  NOT NULL  COMMENT '最终发布版本存储目录',
  verGoodsStorePath  varchar(200)  NOT NULL  COMMENT '历史版本物存储目录'
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='P001版本配置';

/**P002 版本记录[P_VERSION]*/
DROP TABLE IF EXISTS p_Version;
CREATE TABLE p_Version (
  id                int(32)       NOT NULL            AUTO_INCREMENT   COMMENT '版本ID，自动增一',
  appName           varchar(100)  NOT NULL                             COMMENT '应用名称，这里的App不单单值手机应用，也可以看作是App的Id',
  version           varchar(100)  NOT NULL                             COMMENT '版本号，此版本号的规则由程序通过正则表达式进行处理',
  verMemo           text                                               COMMENT '版本描述，可以是一段html',
  bugMemo           text                                               COMMENT '版本bug修改情况描述，可以是一段html',
  pubFlag           int           NOT NULL  DEFAULT 1                  COMMENT '发布状态：0未处理，1已发布，2已撤销，3已作废，-3已作废',
  apkFile           varchar(100)  NOT NULL                             COMMENT '版本发布物的存放地址,目前仅针对apk',
  apkSize           int unsigned  NOT NULL  DEFAULT 0                  COMMENT '版本发布物尺寸大小，是字节数,目前仅针对apk',
  isCurVer          int unsigned  NOT NULL  DEFAULT 0                  COMMENT '是否是当前版本，0不是，1是',
  pubTime           timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '发布时间',
  cTime             timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime            timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  extHisPatchInfo   text                                               COMMENT '历史版本的修改信息,删除版本时，用此保存被删除版本的说明',
  PRIMARY KEY (id),
  UNIQUE INDEX idx_Ver(version) USING BTREE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='P002版本记录';


/**DataAnalysis**/
/**== 二、用户行为数据 */
/**DA001 用户搜索词统计[DA_USERSEARCHWORD]*/
DROP TABLE IF EXISTS da_UserSearchWord;
CREATE TABLE da_UserSearchWord (
  id         varchar(32)   NOT NULL                             COMMENT '用户词Id',
  ownerType  int unsigned  NOT NULL                             COMMENT '所有者类型',
  ownerId    varchar(32)   NOT NULL                             COMMENT '所有者Id,可能是用户也可能是设备',
  word       varchar(100)  NOT NULL                             COMMENT '搜索词',
  wordLang   varchar(100)  NOT NULL                             COMMENT '搜索词语言类型，系统自动判断，可能是混合类型',
  time1      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '本词本用户首次搜索的时间',
  time2      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '本词本用户最后搜索的时间',
  sumNum     int unsigned  NOT NULL                             COMMENT '搜索次数',
  cTime      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  INDEX bizIdx (ownerType, ownerId, word) USING HASH,
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DA001用户搜索词统计';

/**DA002 用户内容喜欢记录表[DA_USERFAVORITE]*/assetType
DROP TABLE IF EXISTS da_UserFavorite;
CREATE TABLE da_UserFavorite (
  id            varchar(32)   NOT NULL                             COMMENT '用户喜欢Id',
  ownerType     int unsigned  NOT NULL                             COMMENT '所有者类型',
  ownerId       varchar(32)   NOT NULL                             COMMENT '所有者Id',
  resTableName  varchar(200)  NOT NULL                             COMMENT '资源类型Id：1电台；2单体媒体资源；3专辑资源，4栏目',
  resId         varchar(32)   NOT NULL                             COMMENT '资源Id',
  cTime         timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  INDEX bizIdx (ownerType, ownerId, resTableName, resId) USING HASH,
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DA002用户内容喜欢记录表';
/**
 * 说明：目前OwnerType只有100，没有其他值
 */
