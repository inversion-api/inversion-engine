package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.List;

import io.inversion.cloud.utils.Utils;

public class PathRule<R extends PathRule>
{
   protected List<Path> excludePaths = new ArrayList();
   protected List<Path> includePaths = new ArrayList();

   public boolean matchesPath(Path path)
   {
      boolean included = false;
      boolean excluded = false;

      if (includePaths.size() == 0)
      {
         if (excludePaths.size() == 0 || path.size() == 0)
            included = true;
      }
      else
      {
         for (Path includePath : includePaths)
         {
            if (includePath.matches(path))
            {
               included = true;
               break;
            }
         }
      }

      if (included && path.size() > 0)
      {
         for (Path excludePath : excludePaths)
         {
            if (excludePath.matches(path))
            {
               excluded = true;
               break;
            }
         }
      }

      return included && !excluded;
   }

   public List<Path> getIncludePaths()
   {
      return new ArrayList(includePaths);
   }

   public R withIncludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            includePaths.add(new Path(path));
         }
      }
      return (R) this;
   }

   public R withIncludePaths(Path... paths)
   {
      if (paths != null)
      {
         for (Path path : paths)
         {
            includePaths.add(path);
         }
      }
      return (R) this;
   }

   public List<Path> getExcludePaths()
   {
      return new ArrayList(excludePaths);
   }

   public R withExcludePaths(String... paths)
   {
      if (paths != null)
      {
         for (String path : Utils.explode(",", paths))
         {
            excludePaths.add(new Path(path));
         }
      }
      return (R) this;
   }

   public R withExcludePaths(Path... paths)
   {
      for (Path path : paths)
      {
         excludePaths.add(path);
      }
      return (R) this;
   }

}
