/**001 字典组[PLAT_DICTM]*/
DROP TABLE IF EXISTS plat_DictM;
CREATE TABLE plat_DictM (
  id         varchar(32)      NOT NULL             COMMENT '字典组表ID(UUID)',
  ownerId    varchar(32)      NOT NULL             COMMENT '所有者Id',
  ownerType  int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '所有者类型(0-系统,1-用户,2-session)',
  dmName     varchar(200)     NOT NULL             COMMENT '字典组名称',
  nPy        varchar(800)                          COMMENT '名称拼音',
  sort       int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '字典组排序,从大到小排序，越大越靠前',
  isValidate int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  mType      int(1) unsigned  NOT NULL  DEFAULT 3  COMMENT '字典类型(1-系统保留,2-系统,3-自定义)',
  mRef       varchar(4000)                         COMMENT '创建时间',
  descn      varchar(500)                          COMMENT '说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime     timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='001字典组';

/**002 字典项[PLAT_DICTD]*/
DROP TABLE IF EXISTS plat_DictD;
CREATE TABLE plat_DictD (
  id         varchar(32)      NOT NULL             COMMENT '字典项表ID(UUID)',
  mId        varchar(32)      NOT NULL             COMMENT '字典组外键(UUID)',
  pId        varchar(32)      NOT NULL             COMMENT '父结点ID(UUID)',
  sort       int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '字典项排序,只在本级排序有意义,从大到小排序，越大越靠前',
  isValidate int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  ddName     varchar(200)     NOT NULL             COMMENT '字典项名称',
  nPy        varchar(800)                          COMMENT '名称拼音',
  aliasName  varchar(200)                          COMMENT '字典项别名',
  anPy       varchar(800)                          COMMENT '别名拼音',
  bCode      varchar(50)      NOT NULL             COMMENT '业务编码',
  dType      int(1) unsigned  NOT NULL  DEFAULT 3  COMMENT '字典类型(1-系统保留,2-系统,3-自定义,4引用-其他字典项ID；)',
  dRef       varchar(4000)                         COMMENT '创建时间',
  descn      varchar(500)                          COMMENT '说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime     timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='002字典项';

/**003 PLAT_USER(用户)*/
DROP TABLE IF EXISTS plat_User;
CREATE TABLE plat_User (
  id             varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  userName       varchar(100)               DEFAULT NULL  COMMENT '用户名称——实名',
  loginName      varchar(15)      NOT NULL                COMMENT '登录账号',
  password       varchar(30)                DEFAULT NULL  COMMENT '密码',
  mailAddress    varchar(100)               DEFAULT NULL  COMMENT '邮箱(非空为一索引)',
  mainPhoneNum   varchar(100)               DEFAULT NULL  COMMENT '用户主手机号码',
  innerPhoneNum  varchar(100)               DEFAULT NULL  COMMENT '内部通话号码，VOIP',
  userType       int(1) unsigned  NOT NULL                COMMENT '用户分类：1自然人用户，2机构用户',
  userState      int(1)           NOT NULL  DEFAULT '0'   COMMENT '用户状态，0-2,0代表未激活的用户，1代表已激用户，2代表失效用户,3根据邮箱找密码的用户',
  protraitBig    varchar(300)                             COMMENT '用户头像大',
  protraitMini   varchar(300)                             COMMENT '用户头像小',
  descn          varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime          timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY loginName(loginName) USING BTREE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='003用户表';

/**004 手机用户使用[WT_MOBILEUSED]*/
/**
 * 记录手机最近一次用某账号登录的情况。
 * 若一个月没有登录，是否就删除掉呢，或者是1年??
 */
DROP TABLE IF EXISTS wt_MobileUsed;
CREATE TABLE wt_MobileUsed (
  id      varchar(32)   NOT NULL  COMMENT 'uuid',
  imei    varchar(100)  NOT NULL  COMMENT '手机串号，手机身份码',
  userId  varchar(32)   NOT NULL  COMMENT '用户Id',
  status  varchar(1)    NOT NULL  COMMENT '状态：1-登录；2-注销；',
  lmTime  timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY imei(imei) USING BTREE

)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='004手机用户使用';

/**005 PLAT_GROUP(用户组)*/
DROP TABLE IF EXISTS plat_Group;
CREATE TABLE plat_Group (
  id             varchar(32)      NOT NULL                COMMENT 'uuid(用户组id)',
  groupNum       varchar(32)                              COMMENT '组号，用于公开的号码',
  groupName      varchar(100)     NOT NULL                COMMENT '组名称',
  groupPwd       varchar(100)     NOT NULL                COMMENT '组密码',
  groupImg       varchar(200)                             COMMENT '用户组头像，是指向头像的URL',
  groupType      int(2) unsigned  NOT NULL  DEFAULT 0     COMMENT '用户组类型,0一般组(由用户根据好友创建);1号码组',
  pId            varchar(32)      NOT NULL  DEFAULT 0     COMMENT '上级用户组名称，默认0，为根',
  sort           int(5) unsigned  NOT NULL  DEFAULT 0     COMMENT '排序,只在本级排序有意义,从大到小排序，越大越靠前',
  createUserId   varchar(32)      NOT NULL                COMMENT '用户组创建者',
  adminUserIds   varchar(32)                              COMMENT '用户组管理者，非一人',
  descn          varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime          timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='005用户组表';
ALTER TABLE `plat_group` ADD UNIQUE INDEX `idxGroupNum` (`groupNum`) USING HASH;
/** 目前和树形组相关的字段pId, sort没有用 */

/**006 PLAT_GROUPUSER(用户组成员)*/
DROP TABLE IF EXISTS plat_GroupUser;
CREATE TABLE plat_GroupUser (
  id       varchar(32)  NOT NULL  COMMENT 'uuid(主键)',
  groupId  varchar(32)  NOT NULL  COMMENT 'uuid(用户组Id)',
  userId   varchar(32)  NOT NULL  COMMENT 'uuid(用户Id)',
  inviter  varchar(32)  NOT NULL  COMMENT 'uuid(邀请者Id)',
  cTime    timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='006用户组成员表';
/** 目前和树型组相关的字段pId, sort没有用 */

/**007 WT_FRIENDINVITE(好友邀请表)*/
DROP TABLE IF EXISTS wt_FriendInvite;
CREATE TABLE wt_FriendInvite (
  id               varchar(32)   NOT NULL                COMMENT 'uuid(主键)',
  aUserId          varchar(32)   NOT NULL                COMMENT '第一用户Id',
  bUserId          varchar(32)   NOT NULL                COMMENT '第二用户Id',
  inviteVector     int(2)        NOT NULL  DEFAULT 0     COMMENT '邀请方向(vector)，总是第一用户邀请第二用户，且是正整数，邀请一次，则增加1，直到邀请成功',
  inviteMessage    varchar(600)                          COMMENT '当前邀请说明文字',
  firstInviteTime  timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:首次邀请时间',
  inviteTime       timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '本次邀请时间',
  acceptFlag       int(1)        NOT NULL  DEFAULT 0     COMMENT '邀请状态：0未处理;1邀请成功;2拒绝邀请',
  acceptTime       timestamp               DEFAULT CURRENT_TIMESTAMP  COMMENT '接受/拒绝邀请的时间',
  refuseMessage    varchar(32)                           COMMENT '拒绝邀请理由',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='007好友邀请列表';

/**008 WT_FRIEND_REL(好友关系表)*/
DROP TABLE IF EXISTS wt_Friend_Rel;
CREATE TABLE wt_Friend_Rel (
  id               varchar(32)  NOT NULL  COMMENT 'uuid(主键)',
  aUserId          varchar(32)  NOT NULL  COMMENT '第一用户Id',
  bUserId          varchar(32)  NOT NULL  COMMENT '第二用户Id',
  inviteVector     varchar(600)           COMMENT '邀请方向(vector)，是正整数，并且表示邀请成功的次数',
  inviteTime       timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '邀请成功的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='008好友列表';
/**此表信息可以根据005表生成，既邀请成功的信息倒入此表*/

/**009 vWT_FRIEND_REL(好友关系试图)*/
CREATE OR REPLACE ALGORITHM=UNDEFINED SQL SECURITY DEFINER
VIEW vWt_Friend_Rel AS 
  select id, aUserId aUserId, bUserId bUserId, 0+inviteVector inviteVector, inviteTime from wt_Friend_Rel
  union all
  select id, bUserId aUserId, aUserId bUserId, 0-inviteVector inviteVector, inviteTime from wt_Friend_Rel
;

/**010 WT_BROADCAST(电台主表)*/
DROP TABLE IF EXISTS wt_Broadcast;
CREATE TABLE wt_Broadcast (
  id            varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  bcTitle       varchar(100)     NOT NULL  COMMENT '电台名称',
  bcPubType     int(1) unsigned  NOT NULL  COMMENT '电台所属类型：1-组织表,2-文本',
  bcPubId       varchar(32)                COMMENT '电台所属集团Id',
  bcPublisher   varchar(100)               COMMENT '电台所属集团',
  bcImg         varchar(100)               COMMENT '电台图标',
  bcURL         varchar(100)               COMMENT '电台网址',
  descn         varchar(4000)              COMMENT '电台说明',
  cTime         timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='010电台主表';

/**011 WT_BCLIVEFLOW(电台直播流子表)*/
DROP TABLE IF EXISTS wt_BCLiveFlow;
CREATE TABLE wt_BCLiveFlow (
  id         varchar(32)      NOT NULL             COMMENT 'uuid(主键)',
  bcId       varchar(32)      NOT NULL             COMMENT '电台Id,外键',
  bcSrcType  int(1) unsigned  NOT NULL             COMMENT '来源，类型：1-组织表；2-文本',
  bcSrcId    varchar(32)                           COMMENT '来源Id，当bcScrType=1',
  bcSource   varchar(100)     NOT NULL             COMMENT '来源，名称',
  flowURI    varchar(300)     NOT NULL             COMMENT '直播流URL',
  isMain     int(1) unsigned  NOT NULL  DEFAULT 0  COMMENT '是否是主直播流；1是主直播流',
  descn      varchar(4000)                         COMMENT '直播流描述',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='011电台直播流子表';

/**012 WT_BCFREQUNCE(电台频段)*/
DROP TABLE IF EXISTS wt_BCFrequnce;
CREATE TABLE wt_BCFrequnce (
  id        varchar(32)   NOT NULL             COMMENT 'uuid(主键)',
  bcId      varchar(32)   NOT NULL             COMMENT '电台Id,外键',
  areaCode  varchar(100)  NOT NULL             COMMENT '地区编码',
  areaName  varchar(100)  NOT NULL             COMMENT '地区名称',
  frequnce  varchar(300)  NOT NULL             COMMENT '频段',
  isMain    integer(1)    NOT NULL  DEFAULT 0  COMMENT '是否是主频段；1是主频段',
  cTime     timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='012电台频段子表';

/**013 WT_APPOPINION(应用意见表)*/
DROP TABLE IF EXISTS wt_AppOpinion;
CREATE TABLE wt_AppOpinion (
  id       varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  imei     varchar(32)   NOT NULL  COMMENT '设备IMEI，为移动端设置，若是PC，则必须是网卡的Mac地址',
  userId   varchar(32)             COMMENT '用户Id',
  opinion  varchar(600)  NOT NULL  COMMENT '所提意见，200汉字',
  cTime    timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，意见成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='013App用户意见表';

/**014 WT_APPREOPINION(应用反馈表)*/
DROP TABLE IF EXISTS wt_AppReOpinion;
CREATE TABLE wt_AppReOpinion (
  id         varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  opinionId  varchar(32)   NOT NULL  COMMENT '意见Id，本反馈是针对那一条意见的',
  userId     varchar(32)   NOT NULL  COMMENT '用户Id，注意这里的用户是员工的Id',
  reOpinion  varchar(600)  NOT NULL  COMMENT '反馈内容，200汉字',
  cTime      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，反馈成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='014App用户意见反馈表';

/*****************************************/
/**
CREATE OR REPLACE ALGORITHM=UNDEFINED SQL SECURITY DEFINER
VIEW vWt_Friend_Rel AS 
  select a.id, a.aUserId aUserId, b.loginName aUserName, b.protraitMini aProtraitUri,
    a.bUserId bUserId, c.loginName bUserName, c.protraitMini bProtraitUri,
    0+a.inviteVector inviteVector, a.inviteTime
  from wt_Friend_Rel a
  left join plat_User b on a.aUserId=b.id
  left join plat_User c on a.bUserId=c.id
  union all
  select d.id, d.bUserId aUserId, e.loginName aUserName, e.protraitMini aProtraitUri,
    d.aUserId bUserId, f.loginName bUserName, f.protraitMini bProtraitUri,
    0-d.inviteVector inviteVector, d.inviteTime
  from wt_Friend_Rel d
  left join plat_User e on d.aUserId=e.id
  left join plat_User f on d.bUserId=f.id
;
**/
/*****************************************/

/**015 WT_MEDIAASSET(媒体资源，文件类聚合，原子性的)*/
DROP TABLE IF EXISTS wt_MediaAsset;
CREATE TABLE wt_MediaAsset (
  id              varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  maTitle         varchar(100)     NOT NULL  COMMENT '媒体资源名称',
  maPubType       int(1) unsigned  NOT NULL  COMMENT '发布类型：1-组织表,2-文本',
  maPubId         varchar(32)      NOT NULL  COMMENT '发布所属组织',
  maPublisher     varchar(100)               COMMENT '发布者',
  maPublisherTime timestamp                  COMMENT '发布时间',
  maImg           varchar(100)               COMMENT '媒体图',
  maURL           varchar(100)               COMMENT '媒体主地址，可以是聚合的源，也可以是Wt平台中的文件URL',
  subjectWord     varchar(400)               COMMENT '主题词',
  keyWord         varchar(400)               COMMENT '关键词',
  langDid         varchar(32)                COMMENT '语言字典项Id',
  language        varchar(32)                COMMENT '语言名称',
  timeLong        long                       COMMENT '时长，毫秒数',
  descn           varchar(4000)              COMMENT '说明',
  cTime           timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='015媒体资源';

/**016 WT_SEQMEDIAASSET(系列媒体资源)*/
DROP TABLE IF EXISTS wt_SeqMediaAsset;
CREATE TABLE wt_SeqMediaAsset (
  id                varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  smaTitle          varchar(100)     NOT NULL  COMMENT '系列媒体资源名称',
  smaPubType        int(1) unsigned  NOT NULL  COMMENT '发布类型：1-组织表,2-文本',
  smaPubId          varchar(32)      NOT NULL  COMMENT '发布所属组织',
  smaPublisher      varchar(100)               COMMENT '发布者',
  smaPublisherTime  timestamp                  COMMENT '发布时间',
  smaImg            varchar(100)               COMMENT '媒体图',
  subjectWord       varchar(400)               COMMENT '主题词',
  keyWord           varchar(400)               COMMENT '关键词',
  langDid           varchar(32)                COMMENT '语言字典项Id',
  language          varchar(32)                COMMENT '语言名称',
  descn             varchar(4000)              COMMENT '说明',
  cTime             timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='016系列媒体资源';

/**017 WT_MASOURCE(资产来源以及播放地址)*/
DROP TABLE IF EXISTS wt_MaSource;
CREATE TABLE wt_MaSource (
  id         varchar(32)      NOT NULL             COMMENT 'uuid(主键)',
  maId       varchar(32)      NOT NULL             COMMENT '媒体Id,外键',
  maSrcTppe  int(1) unsigned  NOT NULL             COMMENT '来源，类型：1-组织表；2-文本',
  maSrcId    varchar(32)      NOT NULL             COMMENT '来源，描述',
  maSource   varchar(100)     NOT NULL             COMMENT '来源，名称',
  smType     int(1) unsigned  NOT NULL             COMMENT '来源媒体分类:1-文件;2-直播流',
  playURI    varchar(300)     NOT NULL             COMMENT '直播流URL',
  isMain     integer(1)       NOT NULL  DEFAULT 0  COMMENT '是否主播放地址；1是主播放',
  descn      varchar(4000)                         COMMENT '来源说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='017资产来源以及播放地址';

/**018 WT_SEQMA_REF(系列媒体与单体媒体对应表)*/
DROP TABLE IF EXISTS wt_SeqMA_Ref;
CREATE TABLE wt_SeqMA_Ref (
  id          varchar(32)    NOT NULL  COMMENT 'uuid(主键)',
  sId         varchar(32)    NOT NULL  COMMENT '系列Id,主表Id',
  mId         varchar(32)    NOT NULL  COMMENT '媒体Id',
  descn       varchar(4000)            COMMENT '关联说明',
  cTime       timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='018系列媒体与单体媒体对应表';

/**019 WT_PERSON(干系人，主要是人的自然信息，和User不同)*/
DROP TABLE IF EXISTS wt_Person;
CREATE TABLE wt_Person (
  id            varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  pName         varchar(100)     NOT NULL                COMMENT '名称',
  age           varchar(15)                              COMMENT '年龄',
  birthday      varchar(30)                DEFAULT NULL  COMMENT '生日',
  sex           varchar(100)                             COMMENT '性别',
  descn         varchar(4000)              DEFAULT NULL  COMMENT '人员描述',
  phoneNum      varchar(100)               DEFAULT NULL  COMMENT '人员手机',
  email         varchar(100)               DEFAULT NULL  COMMENT 'eMail',
  homepage      varchar(100)                             COMMENT '个人主页',
  protraitBig   varchar(300)                             COMMENT '用户头像大',
  protraitMini  varchar(300)                             COMMENT '用户头像小',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='019干系人';

/**020 WT_ORGANIZE(组织机构，和Group不同)*/
DROP TABLE IF EXISTS wt_Organize;
CREATE TABLE wt_Organize (
  id            varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  oName         varchar(100)     NOT NULL                COMMENT '名称',
  descn         varchar(100)               DEFAULT NULL  COMMENT '说明',
  webPage       varchar(100)               DEFAULT NULL  COMMENT '官网地址',
  orgTypeId     varchar(100)                             COMMENT '组织分类Id，可分为：电台、网站等',
  orgTypeName   varchar(100)                             COMMENT '组织分类名称',
  protraitBig   varchar(300)                             COMMENT '组织logo大图像',
  protraitMini  varchar(300)                             COMMENT '组织logo小图像',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='020组织机构';

/**021 WT_MAPERSON_REF(干系人与媒体信息关系)*/
DROP TABLE IF EXISTS wt_MaPerson_Ref;
CREATE TABLE wt_MaPerson_Ref (
  id         varchar(32)    NOT NULL                COMMENT 'uuid(id)',
  personId   varchar(32)    NOT NULL                COMMENT '用户Id',
  resType    varchar(32)    NOT NULL                COMMENT '资源类型Id：1电台；2单体媒体资源；3系列媒体资源',
  resId      varchar(32)    NOT NULL                COMMENT '资源Id',
  refTypeId  varchar(32)    NOT NULL                COMMENT '关联类型，是字典项，是专门的一个字典组',
  cName      varchar(200)   NOT NULL                COMMENT '字典项名称',
  descn      varchar(100)             DEFAULT NULL  COMMENT '描述',
  cTime      timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='021干系人与媒体信息关系';

/**022 WT_RESCATA_REF(资源分类对应关系)*/
DROP TABLE IF EXISTS wt_ResCata_Ref;
CREATE TABLE wt_ResCata_Ref (
  id         varchar(32)    NOT NULL  COMMENT 'uuid(主键)',
  resType    varchar(32)    NOT NULL  COMMENT '资源类型Id：1电台；2单体媒体资源；3系列媒体资源',
  resId      varchar(32)    NOT NULL  COMMENT '资源Id',
  dictMid    varchar(32)    NOT NULL  COMMENT '字典组Id',
  dictMName  varchar(200)   NOT NULL  COMMENT '字典组名称',
  dictDid    varchar(32)    NOT NULL  COMMENT '字典项Id',
  title      varchar(200)   NOT NULL  COMMENT '字典项名称',
  bCode      varchar(200)   NOT NULL  COMMENT '字典项业务编码',
  pathNames  varchar(1000)  NOT NULL  COMMENT '字典项全名称',
  pathIds    varchar(100)   NOT NULL  COMMENT '字典项路径Id',
  cTime      timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='022电台分类表';

/**023 WT_BLACK_GNUM(组号黑名单，名单中号码不会出现在组号中)*/
DROP TABLE IF EXISTS wt_Black_GNum;
CREATE TABLE wt_Black_GNum (
  groupNum  int(16) unsigned  NOT NULL  COMMENT '黑名单号码',
  PRIMARY KEY(groupNum)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='023组号黑名单';
