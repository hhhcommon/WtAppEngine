package com.woting.searchword;

import java.util.List;

import com.spiritdata.framework.util.StringUtils;
import com.woting.searchword.model.Word;

public abstract class WordUtils {
    /**
     * 在中英文混排的字符串中，获取第一个字。
     * 若是中文，就是第一个字
     * 若是英文，则：第一个字
     * @param words
     * @return
     */
    public static String splitFirstWord(String words) {
        if (StringUtils.isNullOrEmptyOrSpace(words)) return null;
        String _words=ToDBC(words);
        char[] ch=_words.toCharArray();
        StringBuffer first=new StringBuffer();
        boolean notChn=false;
        int i=0;
        for (; i<ch.length; i++) {
            char _c=ch[i];
            first.append(_c);

            if (i==0&&isChinese(_c)) break;

            if (notChn&&((_c>'A')&&(_c<'Z'))||(_c==' ')||isChinese(_c)) break;

            if (((_c>'0')&&(_c<'9'))||(_c>'A')&&(_c<'Z')||(_c>'a')&&(_c<'z')) notChn=true;
        }
        if (i>0) first.deleteCharAt(first.length()-1);
        return first.toString();
    }
    /*
     * 全角转半角
     * @param input String.
     * @return 半角字符串
     */
    private static String ToDBC(String input) {
        char c[]=input.toCharArray();
        for (int i=0; i < c.length; i++) {
            if (c[i]=='\u3000') c[i]=' ';
            else
            if ((c[i]>'\uFF00')&&(c[i]<'\uFF5F')) c[i]=(char)(c[i] - 65248);
        }
        String ret=new String(c);
        return ret;
    }
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
           || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
           || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
           || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }

    //以下为对列表排序的处理
    /**
     * 找到词w的在wordL中的插入位置
     * @param w 词
     * @param beginPos 开始位置，必须大于等于0
     * @param endPos 结束位置，必须小于wordL的尺寸
     * @param wordL 词列表，不能为空的列表
     * @return 应插入的位置，若返回-1，则表示有异常
     */
    public static int findInsertPos(Word w, int beginPos, int endPos, List<Word> wordL) {
        int _b=beginPos;
        int _e=endPos;
        if (wordL==null) return -1;
        if (wordL.isEmpty()) return 0;
        if (_e>=wordL.size()) _e=wordL.size()-1;
        if (_b>_e) return -1;

        if (_b==_e||_e-_b==1) {
            if (wordL.get(_b).getCount()<w.getCount()) return _b;
            if (wordL.get(_e).getCount()<w.getCount()) return _e;
            return _e+1;
        }

        int halfIndex=(_b+_e)/2;
        if (wordL.get(halfIndex).getCount()==w.getCount()) return halfIndex+1;
        else if (wordL.get(halfIndex).getCount()>w.getCount()) return findInsertPos(w, halfIndex, _e, wordL);
        else return findInsertPos(w, _b, halfIndex, wordL);
    }
}