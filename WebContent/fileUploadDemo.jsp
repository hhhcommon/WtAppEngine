<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
  String path = request.getContextPath();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta http-equiv="pragma" content="no-cache"/>
<meta http-equiv="cache-control" content="no-cache"/>
<meta http-equiv="expires" content="0"/>
<jsp:include page="/common/sysInclude.jsp" flush="true"/>
<title>测试文件上传</title>
</head>

<body>
<div class="demo-info">
  <div class="demo-tip icon-tip"></div>
  <div>文件上传<input type=button value="添加" onclick="addOneFileSegment()"/></div>
</div>
<div id="fileuploadDiv">
<form method="post" action="/wt/common/upload4App.do" enctype="multipart/form-data" id="multipleUpload" target="tframe">
  <div>
  <input type="file" name="file1"/><input type="text" name="sFile" value="文件名称11"/><input type="button" value="删" onclick="deleteOneFileSegment(this)"/>
  </div>
</form>
<div class="demo-info">
  <div class="demo-tip icon-sum"></div>
  <div><input type=button value="上传ajax" onclick="upload()"/></div>
</div>
</div>
<iframe id="tframe" name="tframe" bordercolor=red frameborder="yes" border=1 width="800" height="400" style="width:800px;heigth:400px; boder:1px solid red;"></iframe>
<!-- 
<form method="post" action="/wt/common/upload4App.do?attrU=attrU" enctype="multipart/form-data" id="multipleUpload" target="wframe">
  <div>
  <input type="file" name="file1"/><input type="text" name="sFile" value="文件名称11"/><input type="button" value="删" onclick="deleteOneFileSegment(this)"/>
  </div>
  <div>
  <input type="file" name="file2"/><input type="text" name="sFile" value="文件名称11"/><input type="button" value="删" onclick="deleteOneFileSegment(this)"/>
  </div>
  <div>
  <input type="file" name="file3"/><input type="text" name="sFile" value="文件名称11"/><input type="button" value="删" onclick="deleteOneFileSegment(this)"/>
  </div>
  <div>
  <input type="text" name="attr1" value="attr1"/>
  <input type="text" name="attr2" value="attr2" />
  </div>
  <input type=submit value="up"/>
</form>
<iframe id="wframe" name="wframe" bordercolor=red frameborder="yes" border=1 width="800" height="400" style="width:800px;heigth:400px; boder:1px solid red;"></iframe>
 -->

</body>
<script>
function addOneFileSegment() {
  var UUID=getUUID();
  $("#fileuploadDiv").find("form").append('<div><input type="file" name="file'+UUID+'"/><input type="text" name="sFile" value="文件名称'+($("#fileuploadDiv").find("form").find("div").length+1)+'"/><input type="button" value="删" onclick="deleteOneFileSegment(this)"/></div>');
}
function deleteOneFileSegment(obj) {
  $(obj).parent().remove();
}
function upload() {
  try {
    var form = $('#multipleUpload');
    $(form).attr('action', _PATH+'/common/upload4App.do');
    $(form).attr('method', 'POST');
    $(form).attr('target', 'tframe');
    if (form.encoding) form.encoding = 'multipart/form-data';    
    else form.enctype = 'multipart/form-data';
    $(form).submit();
  } catch(e) {
  	alert(e);
  }
}
</script>
</html>