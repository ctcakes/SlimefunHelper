// Compose UI integration for the Skia renderer port; upstream renderer attribution in licenses/SuperSoft-renderer-NOTICE.txt.
package me.matl114.gui.nl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.xiamo.gui.SuperSoftComposeScreen
import me.matl114.hacks.api.BaseModule
import me.matl114.hacks.modules.HackModules
import me.matl114.hacks.modules.task.ClickGui
import me.matl114.hacks.utils.config.*
import me.matl114.managers.config.*
import me.matl114.managers.input.MultiKeyBind
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text as McText
import org.lwjgl.glfw.GLFW

private val bg=Color(0xFF0D0F16); private val rail=Color(0xFF12151E); private val panel=Color(0xFF11131B)
private val raised=Color(0xFF272B36); private val border=Color(0xFF262A35); private val fg=Color(0xFFE1E4EC); private val muted=Color(0xFF8F95A3)

class NlSkiaScreen:SuperSoftComposeScreen(McText.literal("SlimefunHelper / SuperSoft Skia")) {
    private var bindTarget:KeyBindRef?=null
    override fun keyPressed(input:KeyInput):Boolean {
        val target=bindTarget ?: return super.keyPressed(input)
        if(input.key()==GLFW.GLFW_KEY_ESCAPE){ target.set(MultiKeyBind()); bindTarget=null; return true }
        if(input.key() in setOf(GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT,GLFW.GLFW_KEY_LEFT_SUPER,GLFW.GLFW_KEY_RIGHT_SUPER)) return true
        val keys=mutableListOf<Int>(); val mods=input.modifiers()
        if(mods and GLFW.GLFW_MOD_CONTROL!=0) keys+=GLFW.GLFW_KEY_LEFT_CONTROL
        if(mods and GLFW.GLFW_MOD_SHIFT!=0) keys+=GLFW.GLFW_KEY_LEFT_SHIFT
        if(mods and GLFW.GLFW_MOD_ALT!=0) keys+=GLFW.GLFW_KEY_LEFT_ALT
        if(mods and GLFW.GLFW_MOD_SUPER!=0) keys+=GLFW.GLFW_KEY_LEFT_SUPER
        keys+=input.key(); runCatching{target.set(MultiKeyBind(*keys.toIntArray()))}; bindTarget=null; return true
    }

    @Composable override fun renderCompose(){
        val groups=remember{HackModules.getModuleGroups().toList()}
        var group by remember{mutableIntStateOf(0)}; var moduleName by remember{mutableStateOf<String?>(null)}
        var scaleOpen by remember{mutableStateOf(false)}; var uiScale by remember{mutableFloatStateOf(1f)}
        var binding by remember{mutableStateOf<KeyBindRef?>(null)}
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000))){
            val scale=(minOf(maxWidth/748.dp,maxHeight/576.dp).coerceIn(.42f,1.4f))*uiScale
            Box(Modifier.align(Alignment.Center).graphicsLayer(scaleX=scale,scaleY=scale).size(748.dp,576.dp).clip(RoundedCornerShape(14.dp)).background(bg)){
                Row(Modifier.fillMaxSize()){
                    Column(Modifier.width(158.dp).fillMaxHeight().background(rail)){
                        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=15.dp),verticalAlignment=Alignment.CenterVertically){
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF081B30)),contentAlignment=Alignment.Center){Text("NL",color=Color(0xFF5EB9FF),fontWeight=FontWeight.Bold)}
                            Column(Modifier.padding(start=8.dp)){Text("Neverlose",color=fg,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Text("SlimefunHelper",color=muted,fontSize=8.sp)}
                        }
                        Divider(color=border)
                        Text("MODULE GROUPS",color=muted,fontSize=9.sp,modifier=Modifier.padding(start=15.dp,top=8.dp,bottom=4.dp))
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=7.dp)){
                            groups.forEachIndexed{i,g->
                            val n=g.javaClass.getDeclaredField("name").let { it.isAccessible=true; it.get(g) as String }; val label=McText.translatableWithFallback("widget.click-gui.module-group-name.${n}",n).string
                                Text(label,color=if(i==group)fg else muted,fontSize=12.sp,modifier=Modifier.fillMaxWidth().padding(vertical=2.dp).clip(RoundedCornerShape(6.dp)).background(if(i==group)raised else Color.Transparent).clickable{group=i;moduleName=null}.padding(9.dp))
                            }
                        }
                        Text("SlimefunHelper • 1.21.11",color=fg,fontSize=9.sp,modifier=Modifier.padding(10.dp).fillMaxWidth().background(raised,RoundedCornerShape(6.dp)).padding(10.dp))
                    }
                    Column(Modifier.weight(1f)){
                        Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF0B0D14)).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
                            Text("SlimefunHelper",color=fg,fontSize=11.sp,modifier=Modifier.width(166.dp).background(panel,RoundedCornerShape(6.dp)).padding(9.dp))
                            Spacer(Modifier.weight(1f))
                            Box{
                                Text("UI  ${(uiScale*100).toInt()}%",color=fg,fontSize=10.sp,modifier=Modifier.clickable{scaleOpen=true}.background(panel,RoundedCornerShape(5.dp)).padding(8.dp))
                                DropdownMenu(expanded=scaleOpen,onDismissRequest={scaleOpen=false}){
                                    Text("GUI scale",color=fg,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
                                    Slider(uiScale,{uiScale=it},valueRange=.65f..1.4f,modifier=Modifier.width(210.dp).padding(horizontal=12.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().weight(1f).padding(start=9.dp,end=10.dp,top=28.dp,bottom=15.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                            val modules=groups.getOrNull(group)?.getModules()?.filter{it.shouldShowInGui()}.orEmpty()
                            val moduleNameOf:(BaseModule)->String={m->m.javaClass.superclass.getDeclaredField("name").let{it.isAccessible=true;it.get(m) as String}}
                            val selected=modules.firstOrNull{moduleNameOf(it)==moduleName}
                            Pane("MODULES",Modifier.weight(55f).fillMaxHeight()){
                                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp)){
                                    modules.forEach{m->val internalName=moduleNameOf(m); Row(Modifier.fillMaxWidth().height(37.dp).clip(RoundedCornerShape(5.dp)).background(if(internalName==moduleName)raised else Color.Transparent).clickable{moduleName=internalName}.padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){
                                        Text(ClickGui.INSTANCE?.getModuleName(m)?.string?:internalName,color=fg,fontSize=11.sp,modifier=Modifier.weight(1f))
                                        m.getBindFlag()?.let{f->Switch(f.get(),{f.toggle()})}
                                    }}
                                }
                            }
                            Pane("SETTINGS",Modifier.weight(45f).fillMaxHeight()){
                                if(selected==null) Text("Select a module",color=muted,fontSize=12.sp,modifier=Modifier.padding(20.dp))
                                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=9.dp)){
                                    selected.getEditableConfig().filter{it.shouldShow()}.forEach{w->
                                        val fields=w.javaClass.getDeclaredFields().associateBy{it.name}.also{it.values.forEach{f->f.isAccessible=true}}
                                        val ref=fields.getValue("ref").get(w) as Ref<*>; val keyName=fields.getValue("keyName").get(w) as String; val label=McText.translatableWithFallback(keyName,keyName).string.let(::settingLabel)
                                        Row(Modifier.fillMaxWidth().heightIn(min=38.dp),verticalAlignment=Alignment.CenterVertically){
                                            Text(label,color=fg,fontSize=11.sp,modifier=Modifier.weight(1f).padding(end=5.dp))
                                            Editor(ref,binding===ref,{k->binding=k;bindTarget=k})
                                        }
                                        Divider(color=border)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun Pane(title:String,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
        Column(modifier.clip(RoundedCornerShape(14.dp)).background(panel).border(1.dp,border,RoundedCornerShape(14.dp))){
            Text(title,color=muted,fontSize=9.sp,modifier=Modifier.padding(start=12.dp,top=7.dp,bottom=2.dp))
            Box(Modifier.fillMaxSize().padding(top=7.dp)){Column(content=content)}
        }
    }
    @Composable private fun Editor(ref:Ref<*>,isBinding:Boolean,onBind:(KeyBindRef)->Unit){
        when(ref){
            is FlagRef->Switch(ref.get(),{ref.toggle()})
            is EnumRef<*>->Pill((ref.getValue() as? ConfigEnum)?.display?.string?:ref.getValue().toString()){NlMainScreen.changeConfig(ref)}
            is KeyBindRef->Pill(if(isBinding)"Press a key…" else ref.get().keyStr.ifBlank{"Click to bind"}){onBind(ref)}
            is NBTRef<*>->NbtEditor(ref)
            else->TextEditor(ref,ref.getAsPrimitive()?.toString().orEmpty())
        }
    }
    @Composable private fun NbtEditor(ref:NBTRef<*>){
        when(val v=ref.getValue()){
            is WrapColor->{
                var open by remember(ref){mutableStateOf(false)}
                val rgb=v.asRGB(); var r by remember(ref){mutableFloatStateOf(((rgb shr 16)and 255)/255f)}
                var g by remember(ref){mutableFloatStateOf(((rgb shr 8)and 255)/255f)}
                var b by remember(ref){mutableFloatStateOf((rgb and 255)/255f)}
                Column(Modifier.width(116.dp)){
                    Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(5.dp)).background(Color(rgb or 0xFF000000.toInt())).border(1.dp,border,RoundedCornerShape(5.dp)).clickable{open=!open})
                    if(open){Text("R",color=fg,fontSize=9.sp);Slider(r,{r=it;color(ref,r,g,b)},Modifier.height(25.dp))
                        Text("G",color=fg,fontSize=9.sp);Slider(g,{g=it;color(ref,r,g,b)},Modifier.height(25.dp))
                        Text("B",color=fg,fontSize=9.sp);Slider(b,{b=it;color(ref,r,g,b)},Modifier.height(25.dp))}
                }
            }
            is Vec2->{
                var x by remember(ref){mutableStateOf(v.x().toString())}
                var y by remember(ref){mutableStateOf(v.y().toString())}
                Row(Modifier.width(116.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
                    TextFieldTiny(x,{x=it;setVec2(ref,x,y)},Modifier.weight(1f))
                    TextFieldTiny(y,{y=it;setVec2(ref,x,y)},Modifier.weight(1f))
                }
            }
            is WidgetPos->{
                var mode by remember(ref){mutableIntStateOf(v.type)}
                var x by remember(ref){mutableStateOf((if(mode==0)v.percentageX else v.lengthX).toString())}
                var y by remember(ref){mutableStateOf((if(mode==0)v.percentageY else v.lengthY).toString())}
                Row(Modifier.width(116.dp),horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(if(mode==0)"PCT" else "ABS",color=fg,fontSize=8.sp,modifier=Modifier.width(34.dp).clickable{mode=1-mode;setPosition(ref,v,mode,x,y)})
                    TextFieldTiny(x,{x=it;setPosition(ref,v,mode,x,y)},Modifier.weight(1f))
                    TextFieldTiny(y,{y=it;setPosition(ref,v,mode,x,y)},Modifier.weight(1f))
                }
            }
            else->TextEditor(ref,ref.getAsPrimitive()?.toString().orEmpty())
        }
    }
    @Composable private fun TextEditor(ref:Ref<*>,initial:String){
        var value by remember(ref){mutableStateOf(initial)}
        TextFieldTiny(value,{value=it;NlMainScreen.setConfig(ref,it)})
    }
    @Composable private fun TextFieldTiny(value:String,onChange:(String)->Unit,modifier:Modifier=Modifier.width(116.dp)){
        BasicTextField(value,onChange,singleLine=true,textStyle=TextStyle(color=fg,fontSize=10.sp),modifier=modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF191C26)).padding(horizontal=5.dp,vertical=6.dp))
    }
    @Composable private fun Pill(value:String,onClick:()->Unit){
        Text(value,color=Color(0xFFC4C7D0),fontSize=9.sp,maxLines=1,modifier=Modifier.width(116.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF191C26)).clickable(onClick=onClick).padding(horizontal=7.dp,vertical=7.dp))
    }
    private fun color(ref:NBTRef<*>,r:Float,g:Float,b:Float){
        NlMainScreen.setConfig(ref,"#%02X%02X%02X".format((r*255).toInt(),(g*255).toInt(),(b*255).toInt()))
    }
    private fun setPosition(ref:NBTRef<*>,v:WidgetPos,mode:Int,x:String,y:String){NlMainScreen.setConfig(ref,"${mode}|${x}|${y}")}
    private fun setVec2(ref:NBTRef<*>,x:String,y:String){NlMainScreen.setConfig(ref,"${x},${y}")}
    private fun settingLabel(s:String):String{
        val i=s.indexOf('：').takeIf{it>=0}?:s.indexOf(':')
        return if(i>=0&&i+1<s.length)s.substring(i+1).trim() else s
    }
}
