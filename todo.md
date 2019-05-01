# letsdo.events

## Tech

- event image upload
- format event date nicely and handle missing values
- event interest
- signup email only
- login email only
- "unjoin" event
- does DB need singular functions?
- validate image format and size
- event list - treat your own event differently
- image resizing
- production build
- hide join button when full
- required & valid mail
- API specs
- data generation via API+specs
- event listing filters
- generate non-conflicting slugs for topic and event
- event times: support invalid dates / try to correct format
- event listing search
- strong password
- include tachyons locally
- db location from config
- config defaults
- limit input char length (against to evil input)
- security headers
- login: redirect to home when already logged in
- catch errors and never show to client
- gzip
- html meta tags
- production router
- cache css
- spec for config
- imprint and cookie warning
- allow editing slug and check for conflicts
- [cookie settings](https://github.com/ring-clojure/ring/wiki/Cookies)
- js as progressive enhancement
- input validation
  - required fields
  - field formats
  - existing email
  - weak password
  - remove additional keys
- login delay against brute force
- error page
- allow app to be running at sub-route instead of top level
- protect form forgery: form nonce
- stats
- [prevent redirect attacks](https://rundis.github.io/blog/2015/buddy_auth_part2.html)
- upload logo
- monitoring/error tracking
- authenticate via mail
- signup via mail
- create a topic
- suggest activity/talk/thing
- event edit view
- topic settings page
- sorting of event list
- use your own colors and logo
- add introduction text
- undo join
- cancel invite
- setup possible dates
- Filter by organizer
- list all your topics
- anyone can join/invite only
- delete topic
- list users
- make someone admin
- change username
- delete account
- single topic mode
- schedule/commit
- redirect unauthorized to login with redirect URI
- edit only by organizer
- multiple organizers per event
- list public topics
- request joining topic
- login theme depending on goto parameter
- Gravatar
- notify about new activity (via mail)
- calendar integration
- API keys
- json export
- interact with events/comments/app via email
- integrate with Meetup.com
  - sync to meetup.com
  - sync form meetup.com
- i18n
- a11y
- use router middleware to fetch topic/event

## Data Model

- topic: name, activity/talk/event
- user: name, email
- invite: email
- admin: topic -> user
- topic-location: topic -> location
- location: title
- activity: title, description, requirements, work-in-progress, start-time, end-time, location, organizer
- topic-activity: topic -> activity
- activity-organizer: user -> activity
- topic-participant: user -> topic
- activity-participant: user -> activity
- login-token: user, token, created-at
- delete event

## Routes

## Pages

### Signup

letsdo.events/login?goto=:route -> login/signup -> POST form

    letsdo.events/login?goto=heart-of-clojure/about/cljdoc-hacking

### Create Topic

letsdo.events/new -> create event -> POST form

### Topic

letsdo.events/for/heart-of-clojure/new -> create event
letsdo.events/for/:topic -> view topic
letsdo.events/for/:topic/about/:event -> view+edit event
letsdo.events/for/:topic/whoareyou -> set user name

### API

POST /api/invite
POST /api/event
PUT /api/event
PUT /api/me
POST /api/join