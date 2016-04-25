/**Plug**/
/**== 一、版本管理 */
/**P001 版本记录[P_VERCONFIG]*/
DROP TABLE IF EXISTS p_VerConfig;
CREATE TABLE p_VerConfig (
  pubStorePath       varchar(200)  NOT NULL  COMMENT '最终版本发布存储目录',
  pubFileName        varchar(200)  NOT NULL  COMMENT '最终版本发布Apk名称',
  pubUrl             varchar(200)  NOT NULL  COMMENT '最终版本发布的Url',
  verGoodsStorePath  varchar(200)  NOT NULL  COMMENT '历史版本发布物存储目录'
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='P001版本配置';

/**P002 版本记录[P_VERSION]*/
DROP TABLE IF EXISTS p_Version;
CREATE TABLE p_Version (
  id        varchar(32)   NOT NULL                             COMMENT '版本ID(UUID)',
  appName   varchar(100)  NOT NULL                             COMMENT '应用名称，这里的App不单单值手机应用',
  verNum    varchar(100)  NOT NULL                             COMMENT '版本号，此版本号的规则由程序通过正则表达式进行处理',
  verMemo   text                                               COMMENT '版本描述，可以是一段html',
  bugMemo   text                                               COMMENT '版本bug修改情况描述，可以是一段html',
  pubFlag   int unsigned  NOT NULL  DEFAULT 1                  COMMENT '发布状态：1=已发布；0=未发布；此状态用于今后扩展，目前只有1',
  apkUrl    varchar(100)  NOT NULL                             COMMENT '版本发布物的访问Url,目前仅针对apk',
  apkSize   int unsigned  NOT NULL  DEFAULT 0                  COMMENT '版本发布物尺寸大小，是字节数,目前仅针对apk',
  isCurVer  int unsigned  NOT NULL  DEFAULT 0                  COMMENT '是否是当前版本，0不是，1是',
  pubTime   timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '发布时间',
  cTime     timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime    timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='P002版本记录';

/**DataAnalysis**/
/**== 二、用户数据 */
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