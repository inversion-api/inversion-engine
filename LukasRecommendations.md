Here are some recommendations for Snooze that I think I can help with immediately.  They are presented in order of what I think is most important to least.

Documentation - Walk-Throughs
I like the Spring Boot example, but I think we should expand it.  I'd recommend an embedded H2 database with sample data that someone can just start playing with immediately.  If they have a database to utilize, let them add it with the Boot app itself via a web interface.

I really enjoy stuff by Baeldung.  All the tutorials include code as well as a working GitHub example.  https://www.baeldung.com/spring-security-remember-me

Spring also does a really good job with their walk-throughs.  
https://spring.io/guides/tutorials/rest/

I think the takeaways are:

1.  Show code in the article.
2.  Have a link to a simple, WORKING example.
3.  Clean layout, easy to read language.

The Master Branch Strategy
If any open-source engineer takes a look at the repo, and there isn't a "master" strategy, they will judge the project harshly.  Anyone should be able to clone master, run a command, and see the latest  and greatest.  I'd recommend we would either:

- Plan release schedules, with all merges to master with code reviews.  On release, cut a release tag.  Master is cutting edge, with nightly releases.

- Merge to master after every release.  Master is most recent GA.

Merge Projects
It looks like you might be trying to split the projects apart.  I think you should go the opposite way.  You can still publish different jars to Maven, but everything that is similar should be in a single repo.  In so doing, people don't need to clone a million repos to see a feature.

I, being a Spring Fanboi, offer it as an example.  Everything is published separately, but you can clone the whole thing in one go.

https://github.com/spring-projects/spring-framework

Use Gradle Wrapper
Add the Gradle Wrapper to all the projects.  It makes things a little easier for people who want to download the project and hit the ground running.  I was always reticent to include a JAR in the past, but it's really a standard now for both Gradle and Maven.

https://stackoverflow.com/questions/20348451/why-should-the-gradle-wrapper-be-committed-to-vcs

Annotation and Programmatic Configuration, 
Include Spring Patterns and Annotations

I feel the current properties configuration is very heavy.  We can still support it, but move towards Spring-like annotations that can be found automatically, as well as YAML support.  I'm unsure of how much you know about those types of configs, so I don't to bombard you with examples, because you probably understand it.

Also, direct Spring Boot integration such as @EnableSnooze with config directly available in Spring application.  We should be able to include our properties directly within the "application-profile" Spring files.

Move to Standard Support Libraries
Apache Commons libraries should become the standard.  Much like having "master" being the latest-and-greatest as part of open source standards, we should embrace the rest of the community libraries.

I agree with your philosophy with keeping dependencies as small as possible.  However, I think we will spend less time worrying about security and other long term problems if we leverage the rest of the community.

Architectural Changes
I haven't dug too much into the architecture itself, but I'm still a little confused as to what an "action" really is.  It seems to be a little mis-named.  We might want to drop the term altogether, and rename it HandlerConfig or something.

This would also involve a general "clean up" of stuff, but I really don't see that much that needs to be changed.  

Documentation - Website 
The new website looks great.  We need to hook it into the Open Source offerings.  It's one thing to say "we are a software company."  It's another to have direct links to your code.


Let's discuss.