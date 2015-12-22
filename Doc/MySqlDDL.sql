/**001 字典组[PLAT_DICTM]*/
DROP TABLE IF EXISTS plat_DictM;
CREATE TABLE plat_DictM (
  id         varchar(32)      NOT NULL             COMMENT '字典组表ID(UUID)',
  ownerId    varchar(32)      NOT NULL             COMMENT '所有者Id',
  ownerType  int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '所有者类型(1-用户,2-session)',
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
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='字典组';

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
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='字典项';

/**003 PLAT_USER(用户)*/
DROP TABLE IF EXISTS plat_User;
CREATE TABLE plat_User (
  id            varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  userName      varchar(100)               DEFAULT NULL  COMMENT '用户名称——实名',
  loginName     varchar(15)      NOT NULL                COMMENT '登录账号',
  password      varchar(30)                DEFAULT NULL  COMMENT '密码',
  mainPhoneNum  varchar(100)               DEFAULT NULL  COMMENT '用户主手机号码',
  mailAddress   varchar(100)               DEFAULT NULL  COMMENT '邮箱(非空为一索引)',
  userType      int(1) unsigned  NOT NULL                COMMENT '用户分类：1自然人用户，2机构用户',
  userState     int(1)           NOT NULL  DEFAULT '0'   COMMENT '用户状态，0-2,0代表未激活的用户，1代表已激用户，2代表失效用户,3根据邮箱找密码的用户',
  protraitBig   varchar(300)                             COMMENT '用户头像大',
  protraitMini  varchar(300)                             COMMENT '用户头像小',
  descn         varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime        timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY loginName(loginName) USING BTREE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

/**004 手机用户使用[PLAT_USER_MOBILEUSED]*/
/**
 * 记录手机最近一次用某账号登录的情况。
 * 若一个月没有登录，是否就删除掉呢，或者是1年??
 */
DROP TABLE IF EXISTS plat_U_MobileUsed;
CREATE TABLE plat_U_MobileUsed (
  id      varchar(32)   NOT NULL  COMMENT 'uuid',
  imei    varchar(100)  NOT NULL  COMMENT '手机串号，手机身份码',
  userId  varchar(32)   NOT NULL  COMMENT '用户Id',
  status  varchar(1)    NOT NULL  COMMENT '状态：1-登录；2-注销；',
  lmTime  timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY imei(imei) USING BTREE

)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='手机用户使用';

/**005 PLAT_GROUP(用户组)*/
DROP TABLE IF EXISTS plat_Group;
CREATE TABLE plat_Group (
  id             varchar(32)      NOT NULL                COMMENT 'uuid(用户组id)',
  groupNum       varchar(100)                             COMMENT '组号，用于公开的号码',
  groupName      varchar(100)     NOT NULL                COMMENT '组名称',
  groupImg       varchar(200)                             COMMENT '用户组头像，是指向头像的URL',
  pId            varchar(32)      NOT NULL  DEFAULT 0     COMMENT '上级用户组名称，默认0，为根',
  sort           int(5) unsigned  NOT NULL  DEFAULT 0     COMMENT '排序,只在本级排序有意义,从大到小排序，越大越靠前',
  createUserId   varchar(32)      NOT NULL                COMMENT '用户组创建者',
  adminUserIds   varchar(32)                              COMMENT '用户组管理者，非一人',
  descn          varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime          timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户组表';
ALTER TABLE `plat_group` ADD UNIQUE INDEX `idxGroupNum` (`groupNum`) USING HASH;
/** 目前和树型组相关的字段pId, sort没有用 */

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
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户组成员表';
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
  acceptTime       timestamp               DEFAULT NULL  COMMENT '接受/拒绝邀请的时间',
  refuseMessage    varchar(32)                           COMMENT '拒绝邀请理由',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='好友邀请列表';

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
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='好友列表';
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
  id          varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  bcTitle     varchar(100)  NOT NULL  COMMENT '电台名称',
  bcPublisher varchar(100)  NOT NULL  COMMENT '电台所属集团名称',
  frequency   varchar(100)  NOT NULL  COMMENT '电台频段，主频段',
  bcImg       varchar(100)            COMMENT '电台图标',
  bcSource    varchar(100)            COMMENT '电台来源',
  flowURI     varchar(100)  NOT NULL  COMMENT '直播流的URI',
  bcURL       varchar(100)            COMMENT '电台网址',
  bcType      varchar(100)            COMMENT '电台分类，对应字典表',
  bcAreaCode  varchar(100)            COMMENT '所属地区编码，对应字典表',
  cTime       timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='电台主表';
/**
  frequency,目前先不涉及频段表
 */

/**011 WT_BROADCAST(电台分类表)*/
DROP TABLE IF EXISTS wt_Broadcast_Catelog;
CREATE TABLE wt_Broadcast_Catelog (
  id       varchar(32)  NOT NULL  COMMENT 'uuid(主键)',
  dictMid  varchar(32)  NOT NULL  COMMENT '字典组Id',
  dictDid  varchar(32)  NOT NULL  COMMENT '字典项Id',
  bcId     varchar(32)  NOT NULL  COMMENT '电台Id',
  cTime    timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='电台分类表';

/**012 WT_APPOPINION(应用意见表)*/
DROP TABLE IF EXISTS wt_AppOpinion;
CREATE TABLE wt_AppOpinion (
  id       varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  imei     varchar(32)   NOT NULL  COMMENT '设备IMEI，为移动端设置，若是PC，则必须是网卡的Mac地址',
  userId   varchar(32)             COMMENT '用户Id',
  opinion  varchar(600)  NOT NULL  COMMENT '所提意见，200汉字',
  cTime    timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，意见成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='电台分类表';

/**013 WT_APPREOPINION(应用反馈表)*/
DROP TABLE IF EXISTS wt_AppReOpinion;
CREATE TABLE wt_AppReOpinion (
  id         varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  opinionId  varchar(32)   NOT NULL  COMMENT '意见Id，本反馈是针对那一条意见的',
  userId     varchar(32)   NOT NULL  COMMENT '用户Id，注意这里的用户是员工的Id',
  reOpinion  varchar(600)  NOT NULL  COMMENT '反馈内容，200汉字',
  cTime      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，反馈成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='电台分类表';



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
