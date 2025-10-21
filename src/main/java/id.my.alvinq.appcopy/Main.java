package id.my.alvinq.appcopy;

import android.content.Context;
import android.widget.Toast;
import java.io.File;

public class Main {
  public static void onLoad(Context context) {
    String pkg = context.getPackageName();
    FileUtils.copyFolder("/data/user/0/" + pkg, "/sdcard/games/user/" + pkg);
    FileUtils.copyFolder("/data/data/" + pkg, "/sdcard/games/data/" + pkg);
    Toast.makeText(context, "Done Copying Files", Toast.LENGTH_LONG).show();
  }
}
