import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ThemeProvider extends ChangeNotifier {
  static Color secondaryBg = Color(0xff262626);

  static ThemeData darkTheme = new ThemeData(
      brightness: Brightness.dark,
      primaryColor: Color(0xff262626),
      accentColor: Color(0xff04CE90),
      backgroundColor: Color(0xff262626),
      textTheme: TextTheme(
        headline: TextStyle(fontSize: 72.0, color: Color(0xffD4D4D4)),
        title: TextStyle(fontSize: 36.0, color: Color(0xffD4D4D4)),
        body1: TextStyle(fontSize: 14.0, color: Color(0xffD4D4D4)),
      ),
      canvasColor: Colors.transparent,
      pageTransitionsTheme: PageTransitionsTheme(
          builders: {
            TargetPlatform.android: CupertinoPageTransitionsBuilder(),
            TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
          }
      ),
      appBarTheme: AppBarTheme(brightness: Brightness.dark, color: Color(0xff262626), elevation: 20));

  static ThemeData lightTheme = new ThemeData(
      brightness: Brightness.light,
      primaryColor: Color(0xff6F4CFF),
      accentColor: Color(0xff5666fa),
      textTheme: TextTheme(
        headline: TextStyle(fontSize: 72.0, color: Colors.grey[800]),
        title: TextStyle(fontSize: 36.0, color: Colors.grey[800]),
        body1: TextStyle(fontSize: 14.0, color: Colors.grey[800]),
      ),
      canvasColor: Colors.transparent,
      appBarTheme: AppBarTheme(brightness: Brightness.dark, color: Color(0xff4d4c5d), elevation: 20));

  ThemeData theme = darkTheme;

  ThemeProvider() {
    SystemChrome.setSystemUIOverlayStyle(
        SystemUiOverlayStyle(statusBarColor: darkTheme.primaryColor, statusBarBrightness: Brightness.light));
  }

  toggleTheme() {
    if (theme.brightness == Brightness.dark) {
      theme = lightTheme;
      SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(statusBarColor: darkTheme.primaryColor));
    } else {
      theme = darkTheme;
      SystemChrome.setSystemUIOverlayStyle(
          SystemUiOverlayStyle(statusBarColor: darkTheme.primaryColor, statusBarBrightness: Brightness.dark));
    }
    notifyListeners();
  }
}
