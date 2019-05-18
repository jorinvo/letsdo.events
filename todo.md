# letsdo.events

- design
  - options via config
    - background-color
    - primary-color
    - text-color
    - title-font
    - base-font

- list view: no join for events you organize or when there is no organizer yet

- sorting of event list
- event listing filters
- event listing search
- error page

- send mail
- redirect empty /for and /about one up
- redirect unauthorized to login with redirect URI
    letsdo.events/login?goto=heart-of-clojure/about/cljdoc-hacking

- allow editing slug and check for conflicts
- image resizing

- default to public topic for now

- list public topics on /

- crop image size in browser

- use same data for schema and for key selection

- spec for config
- package as uberjar

- allow app to be running at sub-route instead of top level
- catch errors and never show to client
- nice error/spec messages for dev
- repair on read: when reading things that are supposed to be unique but are not, throw away invalid ones
  - read user by email: throw away all but the first in time if there is more than one
- delete unused images
  - repair on read: in case an image cannot be found, it must be restored from history

- HTTPS via letsencrypt support

- drag&drop image upload

- allow adding and removing organizers

- confirm delete event
- confirm delete topic

- list all your topics
- list users by topic

- CRUD for topic admin
- multiple organizers per event

- user settings at /me
- delete account

- notify about new activity (via mail) if subscribed to topic
  - admin subscribes automatically
  - can unsubscribe
- notify about updated to activity (via mail) if subscribed
  - joining or interest subscribe automatically
  - can unsubscribe
- calendar integration

- format event date nicely
- event times: support invalid dates / try to correct format
- limit input char length (against evil input)
- spec for API responses
- security headers
- gzip
- html meta tags
- cache css
- imprint and cookie warning
- [cookie settings](https://github.com/ring-clojure/ring/wiki/Cookies)
- check for weak password
- cache pages
- login delay against brute force
- protect form forgery: form nonce
- stats
- [prevent redirect attacks](https://rundis.github.io/blog/2015/buddy_auth_part2.html)
- monitoring/error tracking

- handle display of multiple anonymous organizers

- how to delete unused images?

- manifold vs core.async for transaction

- edit view: image hover

- request joining topic
- anyone can join/invite only
- invite
- cancel invite

- suggest activity/talk/thing
- event interest
- schedule/commit

- Filter by organizer
- confirm() before unjoin

- setup possible dates

- caching headers

- Gravatar
- API keys
- json export
- interact with events/comments/app via email
  - buttons to join, show interest, ...
  - reply to comment
- integrate with Meetup.com
  - sync to meetup.com
  - sync form meetup.com
- i18n
- a11y

- validate image dimension on server-side

- data generation via API+specs
- use API specs for generative testing