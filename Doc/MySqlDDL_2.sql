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


/**LogData**/
/**== 三、日志相关数据 */
/**LD001 消息收发日志**/
DROP TABLE IF EXISTS ld_Message;
CREATE TABLE ld_Message (
  id          varchar(32)   NOT NULL  COMMENT '消息Id',
  flag        varchar(4)    NOT NULL  COMMENT '收发标识，只有两个值send/recv',
  time        bigint        NOT NULL  COMMENT '发送或接收时间，时间戳',
  fromAdress  varchar(400)  NOT NULL  COMMENT '从哪来',
  toAdress    varchar(400)  NOT NULL  COMMENT '到哪去',
  msgStr      text          NOT NULL  COMMENT '消息字符串',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='*LD001消息收发日志表';

/**LD002 音频消息收发**/
DROP TABLE IF EXISTS ld_Audio;
CREATE TABLE ld_Audio (
  id           varchar(32)   NOT NULL  COMMENT '消息Id，可以和ld_Message中互查',
  flag         varchar(4)    NOT NULL  COMMENT '收发标识，只有两个值send/recv',
  time         bigint        NOT NULL  COMMENT '发送或接收时间，时间戳',
  sendTime     bigint        NOT NULL  COMMENT '包中记录的发送或接收时间，时间戳',
  fromAddress  varchar(400)  NOT NULL  COMMENT '从哪来',
  toAddress    varchar(400)  NOT NULL  COMMENT '到哪去',
  seqNo        int           NOT NULL  COMMENT '流数据包编号',
  audioType    varchar(100)  NOT NULL  COMMENT '流数据类型：目前有TALK_INTERCOM(组对讲)、ALK_TELPHONE(电话)',
  talkId       varchar(32)   NOT NULL  COMMENT '会话的Id',
  audioStr     text          NOT NULL  COMMENT '音频数据',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='LD002音频消息收发表';

/** LD003 API处理收集数据 **/
DROP TABLE IF EXISTS ld_API;
CREATE TABLE ld_API (
  id           varchar(32)      NOT NULL             COMMENT '表ID(UUID)',
  reqUrl       varchar(500)                          COMMENT '请求的URL',
  method       varchar(20)                           COMMENT '请求方式是POST/GET/DEL等',
  reqParam     varchar(1000)                         COMMENT '请求中的参数，形式为JSON',
  apiName      varchar(200)     NOT NULL             COMMENT 'Api名称',
  ownerType    int unsigned     NOT NULL             COMMENT '用户类型(1xx:系统;2xx:用户;)',
  ownerId      varchar(32)      NOT NULL             COMMENT '用户Id或SessionID(或指向用户表)，引起文件生成的用户，可以是系统sys',
  deviceType   int(2) unsigned  NOT NULL             COMMENT '设备类型',
  deviceId     varchar(100)                          COMMENT '设备ID(移动端是IMEI,PC是SessionId)',
  deviceClass  varchar(100)                          COMMENT '设备型号',
  exploreName  varchar(100)                          COMMENT '浏览器名称',
  exploreVer   varchar(100)                          COMMENT '浏览器型号',
  objType      varchar(100)                          COMMENT '主对象类型，是数据库模型中主对象的编号',
  objId        varchar(32)                           COMMENT '访问实体的ID',
  dealFlag     int(1) unsigned  NOT NULL  DEFAULT 0  COMMENT '处理过程0正在处理1处理成功2处理失败',
  returnData   text                                  COMMENT '返回数据，以JSON形式',
  beginTime    timestamp                             COMMENT '开始处理时间',
  endTime      timestamp                             COMMENT '结束处理时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='LD003 API处理收集数据';
